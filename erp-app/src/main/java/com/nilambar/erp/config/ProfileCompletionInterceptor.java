package com.nilambar.erp.config;

import com.nilambar.erp.domain.User;
import com.nilambar.erp.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Forces a freshly verified user to finish the profile + address step before anything else.
 */
public class ProfileCompletionInterceptor implements HandlerInterceptor {

    private final CurrentUserService currentUserService;

    public ProfileCompletionInterceptor(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/profile/complete") || path.startsWith("/auth") || path.startsWith("/products")
                || path.startsWith("/css") || path.startsWith("/js") || path.startsWith("/images")
                || path.startsWith("/error")) {
            return true;
        }

        Optional<User> user = currentUserService.find();
        if (user.isPresent() && !user.get().isProfileCompleted()) {
            response.sendRedirect(request.getContextPath() + "/profile/complete");
            return false;
        }
        return true;
    }
}
