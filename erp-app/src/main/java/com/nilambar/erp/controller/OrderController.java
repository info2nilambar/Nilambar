package com.nilambar.erp.controller;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.dto.FeedbackForm;
import com.nilambar.erp.security.CurrentUserService;
import com.nilambar.erp.service.FeedbackService;
import com.nilambar.erp.service.OrderService;
import com.nilambar.erp.service.payment.PaymentQrService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final FeedbackService feedbackService;
    private final PaymentQrService paymentQrService;
    private final CurrentUserService currentUserService;
    private final ErpProperties properties;

    public OrderController(OrderService orderService,
                           FeedbackService feedbackService,
                           PaymentQrService paymentQrService,
                           CurrentUserService currentUserService,
                           ErpProperties properties) {
        this.orderService = orderService;
        this.feedbackService = feedbackService;
        this.paymentQrService = paymentQrService;
        this.currentUserService = currentUserService;
        this.properties = properties;
    }

    @GetMapping
    public String list(Model model) {
        User user = currentUserService.require();
        model.addAttribute("orders", orderService.historyFor(user));
        return "order/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        CustomerOrder order = orderService.require(currentUserService.require(), id);
        model.addAttribute("order", order);
        model.addAttribute("payment", properties.getPayment());
        model.addAttribute("feedback", feedbackService.forOrder(id).orElse(null));
        if (!model.containsAttribute("feedbackForm")) {
            model.addAttribute("feedbackForm", new FeedbackForm());
        }
        return "order/detail";
    }

    @PostMapping("/{id}/feedback")
    public String submitFeedback(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("feedbackForm") FeedbackForm form,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return detail(id, model);
        }
        feedbackService.submit(currentUserService.require(), id, form.getRating(), form.getComment());
        redirectAttributes.addFlashAttribute("infoMessage", "Thanks for your feedback.");
        return "redirect:/orders/" + id;
    }

    @GetMapping(value = "/{id}/payment-qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public byte[] paymentQr(@PathVariable("id") Long id) {
        CustomerOrder order = orderService.require(currentUserService.require(), id);
        return paymentQrService.pngFor(order.getTotal(), order.getOrderNumber());
    }
}
