package com.nilambar.erp.security;

import com.nilambar.erp.domain.User;
import com.nilambar.erp.service.BusinessException;
import com.nilambar.erp.service.UserService;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final UserService userService;

    public CurrentUserService(UserService userService) {
        this.userService = userService;
    }

    public Optional<User> find() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.of(userService.requireByMobile(authentication.getName()));
    }

    public User require() {
        return find().orElseThrow(() -> new BusinessException("You must be signed in to continue."));
    }
}
