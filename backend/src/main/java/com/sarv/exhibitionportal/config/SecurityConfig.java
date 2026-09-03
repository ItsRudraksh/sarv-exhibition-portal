package com.sarv.exhibitionportal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecurityFilterChain staffApi(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/favicon.svg",
                                "/icons.svg",
                                "/assets/**",
                                "/staff",
                                "/staff/**",
                                "/error")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/inquiries/**", "/api/v1/taxonomy/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/staff/me").authenticated()
                        .requestMatchers("/api/v1/staff/suppliers/**")
                        .hasAnyRole("ADMIN", "SUPPLIER_REVIEWER")
                        .requestMatchers("/api/v1/staff/buyers/**")
                        .hasAnyRole("ADMIN", "MARKETING")
                        .requestMatchers("/api/v1/staff/exports/**")
                        .hasAnyRole("ADMIN", "EXPORTER", "MARKETING")
                        .requestMatchers("/api/v1/staff/**").hasRole("ADMIN")
                        .anyRequest().denyAll());
        return http.build();
    }
}
