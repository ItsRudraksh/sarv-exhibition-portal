package com.sarv.exhibitionportal.config;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Production fail-closed checks. Does not invent CRM/SSO — only refuses unsafe local defaults.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductionStartupGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionStartupGuard.class);

    private final ExhibitionProperties properties;
    private final Environment environment;

    public ProductionStartupGuard(ExhibitionProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        if (!prod) {
            return;
        }
        if (properties.poc()) {
            throw new IllegalStateException(
                    "exhibition.poc must be false when the prod profile is active.");
        }
        String password = properties.staffBootstrapPassword();
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "EXHIBITION_STAFF_BOOTSTRAP_PASSWORD is required in prod.");
        }
        if ("poc-staff".equals(password) || "change-me-staff".equals(password)) {
            throw new IllegalStateException(
                    "EXHIBITION_STAFF_BOOTSTRAP_PASSWORD must not be a POC/placeholder value in prod.");
        }
        String forced = properties.outbox() == null ? "" : properties.outbox().forceFailureCode();
        if (forced != null && !forced.isBlank()) {
            throw new IllegalStateException(
                    "exhibition.outbox.force-failure-code must be empty in prod.");
        }
        log.info("Production startup checks passed (poc={}, referencePrefix={})",
                properties.poc(), properties.referencePrefix());
    }
}
