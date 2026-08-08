package com.nilambar.erp.controller;

import com.nilambar.erp.domain.User;
import com.nilambar.erp.security.CurrentUserService;
import com.nilambar.erp.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    public CartController(CartService cartService, CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String view(Model model) {
        User user = currentUserService.require();
        model.addAttribute("cart", cartService.getOrCreateCart(user));
        return "cart/view";
    }

    @PostMapping("/add")
    public String add(@RequestParam("productId") Long productId,
                      @RequestParam(value = "quantity", defaultValue = "1") int quantity,
                      RedirectAttributes redirectAttributes) {
        cartService.addItem(currentUserService.require(), productId, quantity);
        redirectAttributes.addFlashAttribute("infoMessage", "Added to cart.");
        return "redirect:/cart";
    }

    @PostMapping("/items/{id}")
    public String update(@PathVariable("id") Long itemId,
                         @RequestParam("quantity") int quantity,
                         RedirectAttributes redirectAttributes) {
        cartService.updateQuantity(currentUserService.require(), itemId, quantity);
        redirectAttributes.addFlashAttribute("infoMessage", "Cart updated.");
        return "redirect:/cart";
    }

    @PostMapping("/items/{id}/remove")
    public String remove(@PathVariable("id") Long itemId, RedirectAttributes redirectAttributes) {
        cartService.removeItem(currentUserService.require(), itemId);
        redirectAttributes.addFlashAttribute("infoMessage", "Item removed.");
        return "redirect:/cart";
    }
}
