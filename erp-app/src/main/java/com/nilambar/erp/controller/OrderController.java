package com.nilambar.erp.controller;

import com.nilambar.erp.config.ErpProperties;
import com.nilambar.erp.domain.CustomerOrder;
import com.nilambar.erp.domain.OrderReturn;
import com.nilambar.erp.domain.OrderStatus;
import com.nilambar.erp.domain.ReturnReason;
import com.nilambar.erp.domain.User;
import com.nilambar.erp.dto.FeedbackForm;
import com.nilambar.erp.security.CurrentUserService;
import com.nilambar.erp.service.BusinessException;
import com.nilambar.erp.service.FeedbackService;
import com.nilambar.erp.service.OrderService;
import com.nilambar.erp.service.ReturnService;
import com.nilambar.erp.service.payment.PaymentQrService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final FeedbackService feedbackService;
    private final ReturnService returnService;
    private final PaymentQrService paymentQrService;
    private final CurrentUserService currentUserService;
    private final ErpProperties properties;

    public OrderController(OrderService orderService,
                           FeedbackService feedbackService,
                           ReturnService returnService,
                           PaymentQrService paymentQrService,
                           CurrentUserService currentUserService,
                           ErpProperties properties) {
        this.orderService = orderService;
        this.feedbackService = feedbackService;
        this.returnService = returnService;
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
        model.addAttribute("returns", returnService.forOrder(id));
        addReturnAttributes(model, order);
        return "order/detail";
    }

    @GetMapping("/{id}/return")
    public String returnForm(@PathVariable("id") Long id, Model model) {
        CustomerOrder order = orderService.require(currentUserService.require(), id);
        model.addAttribute("order", order);
        model.addAttribute("reasons", ReturnReason.values());
        addReturnAttributes(model, order);
        return "order/return";
    }

    private void addReturnAttributes(Model model, CustomerOrder order) {
        Map<Long, Integer> returnable = returnService.returnableQuantities(order);
        boolean delivered = order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.PICKED_UP;
        model.addAttribute("returnable", returnable);
        model.addAttribute("returnWindowDays", properties.getReturns().getWindowDays());
        model.addAttribute("returnAllowed", delivered && returnService.isWithinReturnWindow(order)
                && returnable.values().stream().anyMatch(quantity -> quantity > 0));
    }

    @PostMapping("/{id}/return")
    public String submitReturn(@PathVariable("id") Long id,
                               @RequestParam("reason") ReturnReason reason,
                               @RequestParam(value = "comment", required = false) String comment,
                               @RequestParam Map<String, String> allParams,
                               RedirectAttributes redirectAttributes) {
        OrderReturn created = returnService.requestReturn(currentUserService.require(), id,
                quantities(allParams), reason, comment);
        redirectAttributes.addFlashAttribute("infoMessage",
                "Return " + created.getReturnNumber() + " requested. We will refund "
                        + created.getRefundAmount().toPlainString() + " once the pickup is processed.");
        return "redirect:/orders/" + id;
    }

    /** Form fields are posted as {@code quantity_<orderItemId>=<n>}. */
    private Map<Long, Integer> quantities(Map<String, String> params) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (!key.startsWith("quantity_") || value == null || value.isBlank()) {
                return;
            }
            try {
                quantities.put(Long.valueOf(key.substring("quantity_".length())), Integer.valueOf(value.trim()));
            } catch (NumberFormatException ignored) {
                throw new BusinessException("Return quantities must be whole numbers.");
            }
        });
        return quantities;
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
