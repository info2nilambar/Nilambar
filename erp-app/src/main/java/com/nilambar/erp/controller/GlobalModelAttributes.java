package com.nilambar.erp.controller;

import com.nilambar.erp.domain.User;
import com.nilambar.erp.security.CurrentUserService;
import com.nilambar.erp.service.CartService;
import java.util.Optional;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CurrentUserService currentUserService;
    private final CartService cartService;

    public GlobalModelAttributes(CurrentUserService currentUserService, CartService cartService) {
        this.currentUserService = currentUserService;
        this.cartService = cartService;
    }

    @ModelAttribute("currentUser")
    public User currentUser() {
        return currentUserService.find().orElse(null);
    }

    @ModelAttribute("cartCount")
    public int cartCount() {
        Optional<User> user = currentUserService.find();
        return user.map(value -> cartService.getOrCreateCart(value).getTotalQuantity()).orElse(0);
    }
}
