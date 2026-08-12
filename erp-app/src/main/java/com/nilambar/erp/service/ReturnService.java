package com.nilambar.erp.service;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.OrderItem;
import com.nilambar.erp.domain.OrderReturn;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.ReturnItem;
import com.nilambar.erp.domain.ReturnReason;
import com.nilambar.erp.domain.ReturnStatus;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.event.ReturnRequestedEvent;
import com.nilambar.erp.messaging.EventPublisher;
import com.nilambar.erp.repository.OrderReturnRepository;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.service.payment.PaymentResult;
import com.nilambar.erp.service.payment.PaymentService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReturnService {

    private static final Logger log = LoggerFactory.getLogger(ReturnService.class);

    private final OrderReturnRepository returnRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final EventPublisher eventPublisher;
    private final ErpProperties properties;
    private final Clock clock;

    public ReturnService(OrderReturnRepository returnRepository,
                         ProductRepository productRepository,
                         OrderService orderService,
                         PaymentService paymentService,
                         EventPublisher eventPublisher,
                         ErpProperties properties,
                         Clock clock) {
        this.returnRepository = returnRepository;
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<OrderReturn> forOrder(Long orderId) {
        return returnRepository.findByOrderIdOrderByIdDesc(orderId);
    }

    /**
     * Quantity of each order item that may still be returned, i.e. the ordered quantity minus what
     * earlier non-rejected returns already claimed.
     */
    @Transactional(readOnly = true)
    public Map<Long, Integer> returnableQuantities(CustomerOrder order) {
        Map<Long, Integer> claimed = new HashMap<>();
        for (OrderReturn existing : returnRepository.findByOrderIdOrderByIdDesc(order.getId())) {
            if (existing.getStatus() == ReturnStatus.REJECTED) {
                continue;
            }
            for (ReturnItem item : existing.getItems()) {
                claimed.merge(item.getOrderItem().getId(), item.getQuantity(), Integer::sum);
            }
        }
        Map<Long, Integer> returnable = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            returnable.put(item.getId(), item.getQuantity() - claimed.getOrDefault(item.getId(), 0));
        }
        return returnable;
    }

    @Transactional(readOnly = true)
    public boolean isWithinReturnWindow(CustomerOrder order) {
        Duration window = Duration.ofDays(properties.getReturns().getWindowDays());
        return !clock.instant().isAfter(order.getUpdatedAt().plus(window));
    }

    /**
     * Records a customer return request. Quantities are validated against what is still returnable,
     * so the same unit can never be refunded twice, and the refund itself is left to
     * {@link #settle(Long)} which the Kafka consumer drives.
     */
    @Transactional
    public OrderReturn requestReturn(User user, Long orderId, Map<Long, Integer> quantitiesByOrderItem,
            ReturnReason reason, String comment) {
        CustomerOrder order = orderService.require(user, orderId);
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.PICKED_UP) {
            throw new BusinessException("Only delivered or picked-up orders can be returned.");
        }
        if (!isWithinReturnWindow(order)) {
            throw new BusinessException("The %d-day return window for this order has closed."
                    .formatted(properties.getReturns().getWindowDays()));
        }
        if (reason == null) {
            throw new BusinessException("Select a return reason.");
        }

        Map<Long, Integer> returnable = returnableQuantities(order);
        Instant now = clock.instant();

        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setReturnNumber(generateReturnNumber());
        orderReturn.setOrder(order);
        orderReturn.setStatus(ReturnStatus.REQUESTED);
        orderReturn.setReason(reason);
        orderReturn.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        orderReturn.setCreatedAt(now);
        orderReturn.setUpdatedAt(now);

        BigDecimal refund = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            int quantity = quantitiesByOrderItem.getOrDefault(item.getId(), 0);
            if (quantity <= 0) {
                continue;
            }
            int allowed = returnable.getOrDefault(item.getId(), 0);
            if (quantity > allowed) {
                throw new BusinessException("Only %d unit(s) of %s can still be returned."
                        .formatted(allowed, item.getProductName()));
            }
            ReturnItem returnItem = new ReturnItem();
            returnItem.setOrderItem(item);
            returnItem.setQuantity(quantity);
            returnItem.setRefundAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            orderReturn.addItem(returnItem);
            refund = refund.add(returnItem.getRefundAmount());
        }
        if (orderReturn.getItems().isEmpty()) {
            throw new BusinessException("Select at least one item to return.");
        }
        orderReturn.setRefundAmount(refund);

        OrderReturn saved = returnRepository.save(orderReturn);

        List<ReturnRequestedEvent.Line> lines = saved.getItems().stream()
                .map(item -> new ReturnRequestedEvent.Line(item.getOrderItem().getProduct().getId(),
                        item.getOrderItem().getProductName(), item.getQuantity()))
                .toList();
        eventPublisher.publish(
                properties.getKafka().getTopics().getReturnRequested(),
                saved.getReturnNumber(),
                new ReturnRequestedEvent(UUID.randomUUID().toString(), saved.getId(), saved.getReturnNumber(),
                        order.getId(), order.getOrderNumber(), user.getId(), reason.name(), refund, lines,
                        now.toString()));

        return saved;
    }

    /**
     * Approves the return, restocks the units and issues the (mock) refund. Called from the Kafka
     * consumer and safe to call twice: anything already refunded is left alone.
     */
    @Transactional
    public void settle(Long returnId) {
        OrderReturn orderReturn = returnRepository.findById(returnId)
                .orElseThrow(() -> new BusinessException("Return not found."));
        if (orderReturn.getStatus() != ReturnStatus.REQUESTED) {
            return;
        }
        if (!properties.getReturns().isAutoApprove()) {
            log.info("Return {} awaits manual approval", orderReturn.getReturnNumber());
            return;
        }

        orderReturn.setStatus(ReturnStatus.APPROVED);
        for (ReturnItem item : orderReturn.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getOrderItem().getProduct().getId())
                    .orElse(null);
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            }
        }

        CustomerOrder order = orderReturn.getOrder();
        PaymentResult refund = paymentService.refund(order.getUser().getId(), order.getOrderNumber(),
                orderReturn.getRefundAmount());
        if (refund.successful()) {
            orderReturn.setStatus(ReturnStatus.REFUNDED);
            orderReturn.setRefundReference(refund.reference());
            orderReturn.setResolutionNote("Refund issued to the original payment method.");
        } else {
            orderReturn.setResolutionNote("Refund pending: " + refund.failureReason());
        }
        orderReturn.setUpdatedAt(clock.instant());
        log.info("Return {} for order {} is now {}", orderReturn.getReturnNumber(), order.getOrderNumber(),
                orderReturn.getStatus());
    }

    private String generateReturnNumber() {
        return "RET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
