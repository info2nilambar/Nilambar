package com.nilambar.erp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nilambar.erp.domain.Cart;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.repository.CartRepository;
import com.nilambar.erp.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CartServiceTest {

    private CartService cartService;
    private User user;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        CartRepository cartRepository = mock(CartRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);

        user = new User();
        user.setId(1L);
        user.setMobile("9876543210");

        cart = new Cart();
        cart.setId(10L);
        cart.setUser(user);

        product = new Product();
        product.setId(100L);
        product.setName("Wireless Earbuds Pro");
        product.setPrice(new BigDecimal("4999.00"));
        product.setStockQuantity(3);
        product.setActive(true);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findById(100L)).thenReturn(Optional.of(product));

        cartService = new CartService(cartRepository, productRepository, clock);
    }

    @Test
    void addingTheSameProductTwiceMergesQuantities() {
        cartService.addItem(user, 100L, 1);
        cartService.addItem(user, 100L, 2);

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getTotalQuantity()).isEqualTo(3);
    }

    @Test
    void subtotalMultipliesPriceByQuantity() {
        cartService.addItem(user, 100L, 3);

        assertThat(cart.getSubtotal()).isEqualByComparingTo("14997.00");
    }

    @Test
    void cannotAddMoreThanAvailableStock() {
        assertThatThrownBy(() -> cartService.addItem(user, 100L, 4))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Only 3 unit(s)");
    }

    @Test
    void inactiveProductIsRejected() {
        product.setActive(false);

        assertThatThrownBy(() -> cartService.addItem(user, 100L, 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void zeroQuantityUpdateRemovesTheLine() {
        cartService.addItem(user, 100L, 2);
        cart.getItems().get(0).setId(500L);

        cartService.updateQuantity(user, 500L, 0);

        assertThat(cart.isEmpty()).isTrue();
    }
}
