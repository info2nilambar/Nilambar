package com.nilambar.erp.controller;

import com.nilambar.erp.domain.User;
import com.nilambar.erp.dto.AddressForm;
import com.nilambar.erp.dto.ProfileForm;
import com.nilambar.erp.security.CurrentUserService;
import com.nilambar.erp.service.AddressService;
import com.nilambar.erp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final AddressService addressService;

    public ProfileController(CurrentUserService currentUserService,
                             UserService userService,
                             AddressService addressService) {
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.addressService = addressService;
    }

    @GetMapping("/complete")
    public String completeProfilePage(Model model) {
        User user = currentUserService.require();
        if (user.isProfileCompleted()) {
            return "redirect:/profile/addresses";
        }
        ProfileForm form = new ProfileForm();
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        model.addAttribute("profileForm", form);
        return "profile/complete";
    }

    @PostMapping("/complete")
    public String completeProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                  BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "profile/complete";
        }
        User user = currentUserService.require();
        userService.updateProfile(user.getId(), form.getName(), form.getEmail());
        addressService.create(user, form.getAddress());
        redirectAttributes.addFlashAttribute("infoMessage", "Profile saved. Start shopping!");
        return "redirect:/products";
    }

    @GetMapping("/addresses")
    public String addresses(Model model) {
        User user = currentUserService.require();
        model.addAttribute("addresses", addressService.listFor(user));
        model.addAttribute("addressForm", new AddressForm());
        return "profile/addresses";
    }

    @PostMapping("/addresses")
    public String addAddress(@Valid @ModelAttribute("addressForm") AddressForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        User user = currentUserService.require();
        if (bindingResult.hasErrors()) {
            model.addAttribute("addresses", addressService.listFor(user));
            return "profile/addresses";
        }
        addressService.create(user, form);
        redirectAttributes.addFlashAttribute("infoMessage", "Address added.");
        return "redirect:/profile/addresses";
    }

    @PostMapping("/addresses/{id}/default")
    public String makeDefault(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        addressService.makeDefault(currentUserService.require(), id);
        redirectAttributes.addFlashAttribute("infoMessage", "Default address updated.");
        return "redirect:/profile/addresses";
    }

    @PostMapping("/addresses/{id}/delete")
    public String delete(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        addressService.delete(currentUserService.require(), id);
        redirectAttributes.addFlashAttribute("infoMessage", "Address removed.");
        return "redirect:/profile/addresses";
    }
}
