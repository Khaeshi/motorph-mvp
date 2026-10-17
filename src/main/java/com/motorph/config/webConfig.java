package com.motorph.config;

import com.motorph.interceptor.AuditLogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuditLogInterceptor auditLogInterceptor;

    public WebConfig(AuditLogInterceptor auditLogInterceptor) {
        this.auditLogInterceptor = auditLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditLogInterceptor)
                .addPathPatterns("/**")
                // EDIT: your login/logout paths (check AuthController mappings)
                .excludePathPatterns("/api/auth/**", "/auth/**", "/login", "/logout", "/error");
    }
}