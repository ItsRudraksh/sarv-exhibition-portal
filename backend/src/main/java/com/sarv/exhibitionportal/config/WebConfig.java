package com.sarv.exhibitionportal.config;

import java.util.Arrays;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ExhibitionProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final ExhibitionProperties properties;

    public WebConfig(ExhibitionProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] configured = properties.corsOrigins() == null || properties.corsOrigins().isEmpty()
                ? new String[] {"https://localhost:5173"}
                : properties.corsOrigins().toArray(String[]::new);
        String[] exact = Arrays.stream(configured)
                .filter(origin -> origin != null && !origin.contains("*"))
                .toArray(String[]::new);
        String[] patterns = Arrays.stream(configured)
                .filter(origin -> origin != null && origin.contains("*"))
                .toArray(String[]::new);
        var mapping = registry.addMapping("/api/**")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(false);
        if (exact.length > 0) {
            mapping.allowedOrigins(exact);
        }
        if (patterns.length > 0) {
            mapping.allowedOriginPatterns(patterns);
        }
    }
}
