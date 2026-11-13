package com.cinebook.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS cho frontend Next.js: cho phép trình duyệt ở cinebook.frontend-url gọi /api/**.
 * (Server component của Next gọi API từ phía server nên không cần CORS; client component thì cần.)
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cinebook.frontend-url}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendUrl)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowCredentials(true)   // cookie phiên khách (FR-301)
                .maxAge(3600);
    }
}
