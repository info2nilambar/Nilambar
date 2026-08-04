package com.nilambar.erp.config;

import com.nilambar.erp.security.CurrentUserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserService currentUserService;

    public WebMvcConfig(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ProfileCompletionInterceptor(currentUserService));
    }
}
