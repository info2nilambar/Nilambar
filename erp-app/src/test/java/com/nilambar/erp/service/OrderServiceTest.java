package com.nilambar.erp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.Address;
import com.nilambar.erp.domain.Cart;
import com.nilambar.erp.domain.CartItem;
import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.FulfilmentType;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.Store;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.messaging.EventPublisher;
import com.nilambar.erp.repository.OrderRepository;
import com.nilambar.erp.repository.ProductRepository;
import com.nilambar.erp.service.delivery.DeliveryQuote;
import com.nilambar.erp.service.delivery.DeliveryService;
import com.nilambar.erp.service.payment.MockPaymentService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private OrderService orderService;
    private MockPaymentService paymentService;
    private DeliveryService deliveryService;
    private User user;
    private Cart cart;
    private Product product;
    private Address address;
    private Store store;

    @BeforeEach
    void setUp() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        CartService cartService = mock(CartService.class);
        AddressService addressService = mock(AddressService.class);
        deliveryService = mock(DeliveryService.class);
        paymentService = new MockPaymentService();

        ErpProperties properties = new ErpProperties();
        properties.getDelivery().setFee(new BigDecimal("40.00"));

        user = new User();
        user.setId(1L);

        product = new Product();
        product.setId(100L);
        product.setName("Bluetooth Speaker 20W");
        product.setPrice(new BigDecimal("2499.00"));
        product.setStockQuantity(5);

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(2);

        cart = new Cart();
        cart.setUser(user);
        cart.getItems().add(item);

        address = new Address();
        address.setId(7L);
        address.setLine1("12 MG Road");
        address.setCity("Bengaluru");
        address.setState("Karnataka");
        address.setPincode("560001");
        address.setLatitude(12.9800);
        address.setLongitude(77.6000);

        store = new Store();
        store.setId(1L);
        store.setName("Central Hub - MG Road");

        when(cartService.getOrCreateCart(user)).thenReturn(cart);
        when(addressService.require(user, 7L)).thenReturn(address);
        when(productRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService = new OrderService(orderRepository, productRepository, cartService, addressService,
                deliveryService, paymentService, mock(EventPublisher.class), properties,
                Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void placingAnOrderDecrementsStockAndTotalsTheCart() {
        givenQuote(true, 2.4);

        CustomerOrder order = orderService.placeOrder(user, 7L, FulfilmentType.HOME_DELIVERY);

        assertThat(product.getStockQuantity()).isEqualTo(3);
        assertThat(order.getSubtotal()).isEqualByComparingTo("4998.00");
        assertThat(order.getDeliveryFee()).isEqualByComparingTo("40.00");
        assertThat(order.getTotal()).isEqualByComparingTo("5038.00");
        assertThat(order.getPaymentReference()).startsWith("MOCKPAY-");
        assertThat(order.getDistanceKm()).isEqualTo(2.4);
    }

    @Test
    void homeDeliveryIsRefusedBeyondTheRadiusEvenWhenRequested() {
        givenQuote(false, 8.2);

        assertThatThrownBy(() -> orderService.placeOrder(user, 7L, FulfilmentType.HOME_DELIVERY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("beyond");
        assertThat(product.getStockQuantity()).isEqualTo(5);
    }

    @Test
    void pickupIsAllowedBeyondTheRadius() {
        givenQuote(false, 8.2);

        CustomerOrder order = orderService.placeOrder(user, 7L, FulfilmentType.STORE_PICKUP);

        assertThat(order.getFulfilmentType()).isEqualTo(FulfilmentType.STORE_PICKUP);
        assertThat(order.getDeliveryFee()).isEqualByComparingTo("0.00");
    }

    @Test
    void insufficientStockAbortsTheOrder() {
        givenQuote(true, 1.0);
        product.setStockQuantity(1);

        assertThatThrownBy(() -> orderService.placeOrder(user, 7L, FulfilmentType.HOME_DELIVERY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only 1 unit(s)");
    }

    @Test
    void emptyCartIsRejected() {
        givenQuote(true, 1.0);
        cart.getItems().clear();

        assertThatThrownBy(() -> orderService.placeOrder(user, 7L, FulfilmentType.HOME_DELIVERY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cart is empty");
    }

    @Test
    void declinedPaymentAbortsTheOrder() {
        givenQuote(true, 1.0);
        paymentService.setForceFailure(true);

        assertThatThrownBy(() -> orderService.placeOrder(user, 7L, FulfilmentType.HOME_DELIVERY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment failed");
    }

    private void givenQuote(boolean homeDelivery, double distanceKm) {
        DeliveryQuote quote = new DeliveryQuote(homeDelivery, store, distanceKm, 5,
                homeDelivery ? new BigDecimal("40.00") : BigDecimal.ZERO,
                homeDelivery ? FulfilmentType.HOME_DELIVERY : FulfilmentType.STORE_PICKUP,
                homeDelivery ? "Home delivery available." : "This address is beyond the 5 km radius.");
        when(deliveryService.quoteFor(address)).thenReturn(quote);
    }
}
