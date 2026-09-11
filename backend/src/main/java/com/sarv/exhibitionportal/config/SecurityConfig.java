package com.sarv.exhibitionportal.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
/** Public visitor APIs vs role-gated staff. JSON 401 without WWW-Authenticate. */
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * JSON 401 without {@code WWW-Authenticate: Basic}. The browser Basic dialog must never
     * appear on the public visitor URL. Staff/admin use the in-app sign-in form + fetch.
     */
    @Bean
    AuthenticationEntryPoint jsonUnauthorized() {
        return (HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException ex)
                -> writeUnauthorized(response);
    }

    @Bean
    SecurityFilterChain staffApi(HttpSecurity http, AuthenticationEntryPoint jsonUnauthorized) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(jsonUnauthorized))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(jsonUnauthorized))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/favicon.svg",
                                "/icons.svg",
                                "/assets/**",
                                "/tessdata/**",
                                "/web",
                                "/web/**",
                                "/staff",
                                "/staff/**",
                                "/admin",
                                "/admin/**",
                                "/error")
                        .permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(
                                "/api/v1/inquiries/**",
                                "/api/v1/taxonomy/**",
                                "/api/v1/campaigns/**",
                                "/api/v1/meta",
                                "/api/v1/finished-goods/**",
                                "/api/v1/buyer-products/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/staff/me").authenticated()
                        .requestMatchers("/api/v1/staff/suppliers/**")
                        .hasAnyRole("ADMIN", "SUPPLIER_REVIEWER")
                        .requestMatchers("/api/v1/staff/buyers/**")
                        .hasAnyRole("ADMIN", "MARKETING")
                        .requestMatchers("/api/v1/staff/exports/**")
                        .hasAnyRole("ADMIN", "EXPORTER", "MARKETING")
                        .requestMatchers("/api/v1/staff/finished-goods/**")
                        .hasAnyRole("ADMIN", "MARKETING")
                        .requestMatchers("/api/v1/staff/trading-suppliers/**")
                        .hasAnyRole("ADMIN", "TAXONOMY_MANAGER")
                        .requestMatchers("/api/v1/staff/users/**", "/api/v1/staff/roles")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/v1/staff/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .anyRequest().denyAll());
        return http.build();
    }

    private static void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"Sign in required.\"}");
    }
}
