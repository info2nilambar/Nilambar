package com.nilambar.erp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.OrderItem;
import com.nilambar.erp.domain.OrderReturn;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.ReturnReason;
import com.nilambar.erp.domain.ReturnStatus;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.event.ReturnRequestedEvent;
import com.nilambar.erp.messaging.EventPublisher;
import com.nilambar.erp.repository.OrderReturnRepository;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.service.payment.MockPaymentService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReturnServiceTest {

    private static final Instant NOW = Instant.parse("2026-02-10T09:00:00Z");

    private OrderReturnRepository returnRepository;
    private ProductRepository productRepository;
    private EventPublisher eventPublisher;
    private MockPaymentService paymentService;
    private ErpProperties properties;
    private ReturnService returnService;

    private User user;
    private CustomerOrder order;
    private OrderItem item;
    private Product product;

    @BeforeEach
    void setUp() {
        returnRepository = mock(OrderReturnRepository.class);
        productRepository = mock(ProductRepository.class);
        eventPublisher = mock(EventPublisher.class);
        paymentService = new MockPaymentService();
        properties = new ErpProperties();
        OrderService orderService = mock(OrderService.class);

        user = new User();
        user.setId(1L);

        product = new Product();
        product.setId(7L);
        product.setStockQuantity(4);

        item = new OrderItem();
        item.setId(50L);
        item.setProduct(product);
        item.setProductName("Steel Tiffin Box");
        item.setUnitPrice(new BigDecimal("250.00"));
        item.setQuantity(3);

        order = new CustomerOrder();
        order.setId(9L);
        order.setUser(user);
        order.setOrderNumber("ORD-1");
        order.setStatus(OrderStatus.DELIVERED);
        order.setUpdatedAt(NOW.minusSeconds(3600));
        order.getItems().add(item);
        item.setOrder(order);

        when(orderService.require(user, 9L)).thenReturn(order);
        when(returnRepository.findByOrderIdOrderByIdDesc(9L)).thenReturn(List.of());
        when(returnRepository.save(any(OrderReturn.class))).thenAnswer(inv -> {
            OrderReturn saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });
        when(productRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(product));

        returnService = new ReturnService(returnRepository, productRepository, orderService, paymentService,
                eventPublisher, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void requestPricesTheSelectedUnitsAndPublishesAnEvent() {
        OrderReturn created = returnService.requestReturn(user, 9L, Map.of(50L, 2),
                ReturnReason.DAMAGED, "  Dented lid  ");

        assertThat(created.getStatus()).isEqualTo(ReturnStatus.REQUESTED);
        assertThat(created.getRefundAmount()).isEqualByComparingTo("500.00");
        assertThat(created.getComment()).isEqualTo("Dented lid");
        assertThat(created.getReturnNumber()).startsWith("RET-");
        assertThat(created.getItems()).singleElement()
                .satisfies(line -> assertThat(line.getQuantity()).isEqualTo(2));
        verify(eventPublisher).publish(eq("erp.order.return.requested"), anyString(),
                any(ReturnRequestedEvent.class));
    }

    @Test
    void itemsNotSelectedAreRejected() {
        assertThatThrownBy(() -> returnService.requestReturn(user, 9L, Map.of(50L, 0), ReturnReason.OTHER, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    void quantityBeyondWhatWasOrderedIsRejected() {
        assertThatThrownBy(() -> returnService.requestReturn(user, 9L, Map.of(50L, 4), ReturnReason.OTHER, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only 3 unit(s)");
    }

    @Test
    void alreadyReturnedUnitsAreNotReturnableAgain() {
        OrderReturn existing = returnService.requestReturn(user, 9L, Map.of(50L, 2), ReturnReason.OTHER, null);
        when(returnRepository.findByOrderIdOrderByIdDesc(9L)).thenReturn(List.of(existing));

        assertThat(returnService.returnableQuantities(order)).containsEntry(50L, 1);
        assertThatThrownBy(() -> returnService.requestReturn(user, 9L, Map.of(50L, 2), ReturnReason.OTHER, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only 1 unit(s)");
    }

    @Test
    void rejectedReturnsReleaseTheirUnits() {
        OrderReturn existing = returnService.requestReturn(user, 9L, Map.of(50L, 3), ReturnReason.OTHER, null);
        existing.setStatus(ReturnStatus.REJECTED);
        when(returnRepository.findByOrderIdOrderByIdDesc(9L)).thenReturn(List.of(existing));

        assertThat(returnService.returnableQuantities(order)).containsEntry(50L, 3);
    }

    @Test
    void ordersStillInFlightCannotBeReturned() {
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);

        assertThatThrownBy(() -> returnService.requestReturn(user, 9L, Map.of(50L, 1), ReturnReason.OTHER, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("delivered or picked-up");
    }

    @Test
    void returnsAreRefusedOnceTheWindowHasClosed() {
        order.setUpdatedAt(NOW.minusSeconds(8 * 24 * 3600));

        assertThat(returnService.isWithinReturnWindow(order)).isFalse();
        assertThatThrownBy(() -> returnService.requestReturn(user, 9L, Map.of(50L, 1), ReturnReason.OTHER, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("return window");
    }

    @Test
    void settlementRestocksTheUnitsAndRefunds() {
        OrderReturn created = returnService.requestReturn(user, 9L, Map.of(50L, 2), ReturnReason.DAMAGED, null);
        when(returnRepository.findById(100L)).thenReturn(Optional.of(created));

        returnService.settle(100L);

        assertThat(created.getStatus()).isEqualTo(ReturnStatus.REFUNDED);
        assertThat(created.getRefundReference()).startsWith("MOCKRFND-");
        assertThat(product.getStockQuantity()).isEqualTo(6);
    }

    @Test
    void settlementIsIdempotent() {
        OrderReturn created = returnService.requestReturn(user, 9L, Map.of(50L, 2), ReturnReason.DAMAGED, null);
        when(returnRepository.findById(100L)).thenReturn(Optional.of(created));

        returnService.settle(100L);
        returnService.settle(100L);

        assertThat(product.getStockQuantity()).isEqualTo(6);
    }

    @Test
    void failedRefundLeavesTheReturnApprovedWithANote() {
        OrderReturn created = returnService.requestReturn(user, 9L, Map.of(50L, 1), ReturnReason.DAMAGED, null);
        when(returnRepository.findById(100L)).thenReturn(Optional.of(created));
        paymentService.setForceFailure(true);

        returnService.settle(100L);

        assertThat(created.getStatus()).isEqualTo(ReturnStatus.APPROVED);
        assertThat(created.getResolutionNote()).contains("Refund pending");
    }

    @Test
    void manualApprovalModeLeavesTheReturnRequested() {
        properties.getReturns().setAutoApprove(false);
        OrderReturn created = returnService.requestReturn(user, 9L, Map.of(50L, 1), ReturnReason.DAMAGED, null);
        when(returnRepository.findById(100L)).thenReturn(Optional.of(created));

        returnService.settle(100L);

        assertThat(created.getStatus()).isEqualTo(ReturnStatus.REQUESTED);
        verify(productRepository, never()).findByIdForUpdate(7L);
    }
}
