package com.nilambar.erp.controller;

import com.nilambar.erp.domain.Address;
import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.FulfilmentType;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.security.CurrentUserService;
import com.nilambar.erp.service.AddressService;
import com.nilambar.erp.service.CartService;
import com.nilambar.erp.service.OrderService;
import com.nilambar.erp.service.delivery.DeliveryQuote;
import com.nilambar.erp.service.delivery.DeliveryService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartService cartService;
    private final AddressService addressService;
    private final DeliveryService deliveryService;
    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public CheckoutController(CartService cartService,
                              AddressService addressService,
                              DeliveryService deliveryService,
                              OrderService orderService,
                              CurrentUserService currentUserService) {
        this.cartService = cartService;
        this.addressService = addressService;
        this.deliveryService = deliveryService;
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String checkout(@RequestParam(value = "addressId", required = false) Long addressId, Model model) {
        User user = currentUserService.require();
        List<Address> addresses = addressService.listFor(user);

        model.addAttribute("cart", cartService.getOrCreateCart(user));
        model.addAttribute("addresses", addresses);

        if (addresses.isEmpty()) {
            return "checkout/index";
        }

        Address selected = addressId == null
                ? addresses.get(0)
                : addressService.require(user, addressId);
        DeliveryQuote quote = deliveryService.quoteFor(selected);

        model.addAttribute("selectedAddress", selected);
        model.addAttribute("quote", quote);
        return "checkout/index";
    }

    @PostMapping("/place")
    public String placeOrder(@RequestParam("addressId") Long addressId,
                             @RequestParam("fulfilmentType") FulfilmentType fulfilmentType,
                             RedirectAttributes redirectAttributes) {
        CustomerOrder order = orderService.placeOrder(currentUserService.require(), addressId, fulfilmentType);
        redirectAttributes.addFlashAttribute("infoMessage",
                "Order " + order.getOrderNumber() + " placed successfully.");
        return "redirect:/orders";
    }
}
