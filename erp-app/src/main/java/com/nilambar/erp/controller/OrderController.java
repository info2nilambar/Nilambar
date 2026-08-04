package com.nilambar.erp.controller;

import com.nilambar.erp.domain.User;
import com.nilambar.erp.security.CurrentUserService;
import com.nilambar.erp.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;

    public OrderController(OrderService orderService, CurrentUserService currentUserService) {
        this.orderService = orderService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public String list(Model model) {
        User user = currentUserService.require();
        model.addAttribute("orders", orderService.historyFor(user));
        return "order/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        model.addAttribute("order", orderService.require(currentUserService.require(), id));
        return "order/detail";
    }
}
