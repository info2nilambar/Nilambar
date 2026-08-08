package com.nilambar.erp.service;

import com.nilambar.erp.domain.Cart;
import com.nilambar.erp.domain.CartItem;
import com.nilambar.erp.domain.Product;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.repository.CartRepository;
import com.nilambar.erp.repository.ProductRepository;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final Clock clock;

    public CartService(CartRepository cartRepository, ProductRepository productRepository, Clock clock) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.clock = clock;
    }

    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            cart.setUpdatedAt(clock.instant());
            return cartRepository.save(cart);
        });
    }

    @Transactional
    public void addItem(User user, Long productId, int quantity) {
        if (quantity < 1) {
            throw new BusinessException("Quantity must be at least 1.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found."));
        if (!product.isActive()) {
            throw new BusinessException("This product is no longer available.");
        }

        Cart cart = getOrCreateCart(user);
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        int newQuantity = existing.map(CartItem::getQuantity).orElse(0) + quantity;
        if (newQuantity > product.getStockQuantity()) {
            throw new BusinessException("Only %d unit(s) of %s are in stock."
                    .formatted(product.getStockQuantity(), product.getName()));
        }

        if (existing.isPresent()) {
            existing.get().setQuantity(newQuantity);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        }
        cart.setUpdatedAt(clock.instant());
    }

    @Transactional
    public void updateQuantity(User user, Long itemId, int quantity) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cart.getItems().stream()
                .filter(candidate -> candidate.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Cart item not found."));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else if (quantity > item.getProduct().getStockQuantity()) {
            throw new BusinessException("Only %d unit(s) of %s are in stock."
                    .formatted(item.getProduct().getStockQuantity(), item.getProduct().getName()));
        } else {
            item.setQuantity(quantity);
        }
        cart.setUpdatedAt(clock.instant());
    }

    @Transactional
    public void removeItem(User user, Long itemId) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        cart.setUpdatedAt(clock.instant());
    }

    @Transactional
    public void clear(Cart cart) {
        cart.getItems().clear();
        cart.setUpdatedAt(clock.instant());
    }
}
