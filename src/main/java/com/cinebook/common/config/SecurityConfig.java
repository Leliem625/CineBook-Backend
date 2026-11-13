package com.cinebook.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình Spring Security.
 * - Khách hàng không cần đăng nhập: mọi API public mở.
 * - /api/v1/admin/** và /api/v1/staff/** yêu cầu tài khoản nội bộ.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // TODO bật lại CSRF cho cookie khi làm frontend; IPN VNPay phải được bỏ qua CSRF
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())   // dùng cấu hình trong CorsConfig
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/v1/staff/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                .anyRequest().permitAll())
            // TODO giai đoạn đầu: form login với StaffUserDetailsService; giai đoạn cuối: JWT
            .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
