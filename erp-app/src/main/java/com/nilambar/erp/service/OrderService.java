package com.nilambar.erp.service;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.Address;
import com.nilambar.erp.domain.Cart;
import com.nilambar.erp.domain.CartItem;
import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.FulfilmentType;
import com.nilambar.erp.domain.OrderItem;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.event.OrderPlacedEvent;
import com.nilambar.erp.messaging.EventPublisher;
import com.nilambar.erp.repository.OrderRepository;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.service.delivery.DeliveryQuote;
import com.nilambar.erp.service.delivery.DeliveryService;
import com.nilambar.erp.service.payment.PaymentResult;
import com.nilambar.erp.service.payment.PaymentService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final AddressService addressService;
    private final DeliveryService deliveryService;
    private final PaymentService paymentService;
    private final EventPublisher eventPublisher;
    private final ErpProperties properties;
    private final Clock clock;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        CartService cartService,
                        AddressService addressService,
                        DeliveryService deliveryService,
                        PaymentService paymentService,
                        EventPublisher eventPublisher,
                        ErpProperties properties,
                        Clock clock) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.addressService = addressService;
        this.deliveryService = deliveryService;
        this.paymentService = paymentService;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Places an order. The delivery-radius rule is re-evaluated here rather than trusting the
     * fulfilment type posted by the browser.
     */
    @Transactional
    public CustomerOrder placeOrder(User user, Long addressId, FulfilmentType requestedFulfilment) {
        Cart cart = cartService.getOrCreateCart(user);
        if (cart.isEmpty()) {
            throw new BusinessException("Your cart is empty.");
        }

        Address address = addressService.require(user, addressId);
        DeliveryQuote quote = deliveryService.quoteFor(address);

        if (requestedFulfilment == FulfilmentType.HOME_DELIVERY && !quote.homeDeliveryAvailable()) {
            throw new BusinessException(quote.message());
        }
        FulfilmentType fulfilment = quote.homeDeliveryAvailable() ? requestedFulfilment : FulfilmentType.STORE_PICKUP;

        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setAddress(address);
        order.setStore(quote.nearestStore());
        order.setFulfilmentType(fulfilment);
        order.setDistanceKm(quote.distanceKm());
        order.setStatus(OrderStatus.PLACED);
        order.setDeliveryAddressSnapshot(address.getSummary());
        order.setCreatedAt(clock.instant());
        order.setUpdatedAt(clock.instant());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new BusinessException("Product no longer exists."));
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BusinessException("Only %d unit(s) of %s are in stock."
                        .formatted(product.getStockQuantity(), product.getName()));
            }
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            order.addItem(orderItem);

            subtotal = subtotal.add(orderItem.getLineTotal());
        }

        BigDecimal fee = fulfilment == FulfilmentType.HOME_DELIVERY
                ? properties.getDelivery().getFee()
                : properties.getDelivery().getPickupFee();
        order.setSubtotal(subtotal);
        order.setDeliveryFee(fee);
        order.setTotal(subtotal.add(fee));

        PaymentResult payment = paymentService.charge(user.getId(), order.getOrderNumber(), order.getTotal());
        if (!payment.successful()) {
            throw new BusinessException("Payment failed: " + payment.failureReason());
        }
        order.setPaymentReference(payment.reference());

        CustomerOrder saved = orderRepository.save(order);
        cartService.clear(cart);

        List<OrderPlacedEvent.Line> lines = saved.getItems().stream()
                .map(item -> new OrderPlacedEvent.Line(item.getProduct().getId(), item.getProductName(),
                        item.getQuantity(), item.getUnitPrice()))
                .toList();
        eventPublisher.publish(
                properties.getKafka().getTopics().getOrderPlaced(),
                saved.getOrderNumber(),
                new OrderPlacedEvent(UUID.randomUUID().toString(), saved.getId(), saved.getOrderNumber(),
                        user.getId(), fulfilment.name(), saved.getDistanceKm(), saved.getTotal(), lines,
                        saved.getCreatedAt().toString()));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> historyFor(User user) {
        return orderRepository.findByUserIdOrderByIdDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public CustomerOrder require(User user, Long orderId) {
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BusinessException("Order not found."));
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
