package com.example.Validation;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AdminAuthenticationInterceptor adminAuthenticationInterceptor;

    public WebMvcConfig(AdminAuthenticationInterceptor adminAuthenticationInterceptor) {
        this.adminAuthenticationInterceptor = adminAuthenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthenticationInterceptor)
                .addPathPatterns(
                        "/giam-doc", "/giam-doc/**",
                        "/hcns", "/hcns/**",
                        "/cskh", "/cskh/**",
                        "/marketing", "/marketing/**"
                );
    }
}
