package com.nilambar.erp.controller;

import com.nilambar.erp.domain.User;
import com.nilambar.erp.dto.MobileForm;
import com.nilambar.erp.dto.OtpForm;
import com.nilambar.erp.security.SessionAuthenticator;
import com.nilambar.erp.service.BusinessException;
import com.nilambar.erp.service.UserService;
import com.nilambar.erp.service.otp.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final OtpService otpService;
    private final UserService userService;
    private final SessionAuthenticator sessionAuthenticator;

    public AuthController(OtpService otpService, UserService userService, SessionAuthenticator sessionAuthenticator) {
        this.otpService = otpService;
        this.userService = userService;
        this.sessionAuthenticator = sessionAuthenticator;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("mobileForm", new MobileForm());
        return "auth/login";
    }

    @PostMapping("/otp/request")
    public String requestOtp(@Valid @ModelAttribute("mobileForm") MobileForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }
        try {
            otpService.requestOtp(form.getMobile());
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/login";
        }
        redirectAttributes.addFlashAttribute("infoMessage",
                "OTP sent to " + form.getMobile() + ". It is valid for 5 minutes.");
        redirectAttributes.addAttribute("mobile", form.getMobile());
        return "redirect:/auth/verify";
    }

    @GetMapping("/verify")
    public String verifyPage(@RequestParam("mobile") String mobile, Model model) {
        OtpForm form = new OtpForm();
        form.setMobile(mobile);
        model.addAttribute("otpForm", form);
        return "auth/verify";
    }

    @PostMapping("/otp/verify")
    public String verifyOtp(@Valid @ModelAttribute("otpForm") OtpForm form,
                            BindingResult bindingResult,
                            Model model,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return "auth/verify";
        }
        try {
            otpService.verify(form.getMobile(), form.getCode());
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/verify";
        }

        User user = userService.findOrCreate(form.getMobile());
        sessionAuthenticator.authenticate(user, request, response);

        return user.isProfileCompleted() ? "redirect:/products" : "redirect:/profile/complete";
    }
}
