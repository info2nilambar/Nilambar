package com.nilambar.erp.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // JSP views live under /WEB-INF and are reached by a FORWARD dispatch.
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers("/", "/auth/**", "/products", "/products/**", "/css/**", "/js/**", "/images/**", "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(login -> login.loginPage("/auth/login").permitAll().disable())
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/products")
                        .permitAll())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendRedirect(request.getContextPath() + "/auth/login")))
                .csrf(Customizer.withDefaults());
        return http.build();
    }
}
