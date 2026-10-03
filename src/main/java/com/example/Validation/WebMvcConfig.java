package com.example.Validation;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final AdminAuthenticationInterceptor adminAuthenticationInterceptor;
    private final CacheInvalidationInterceptor cacheInvalidationInterceptor;

    public WebMvcConfig(AdminAuthenticationInterceptor adminAuthenticationInterceptor,
                        CacheInvalidationInterceptor cacheInvalidationInterceptor) {
        this.adminAuthenticationInterceptor = adminAuthenticationInterceptor;
        this.cacheInvalidationInterceptor = cacheInvalidationInterceptor;
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

        // Xóa cache Redis sau các thao tác ghi dữ liệu (trang quản trị + API ứng dụng di động)
        registry.addInterceptor(cacheInvalidationInterceptor)
                .addPathPatterns(
                        "/giam-doc/**", "/hcns/**", "/cskh/**", "/marketing/**",
                        "/v1/**"
                );
    }
}
