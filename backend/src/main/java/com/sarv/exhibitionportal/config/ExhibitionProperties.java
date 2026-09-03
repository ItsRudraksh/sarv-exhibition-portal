package com.sarv.exhibitionportal.config;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exhibition")
public record ExhibitionProperties(
        boolean poc,
        UUID defaultCampaignId,
        List<String> corsOrigins,
        String storageRoot,
        int fileRetentionDays,
        long cardMaxBytes,
        long catalogueMaxBytes,
        String consentPolicyVersion,
        int exportRetentionHours,
        Outbox outbox,
        String staffBootstrapPassword
) {
    public ExhibitionProperties {
        if (staffBootstrapPassword == null) {
            staffBootstrapPassword = "";
        }
    }

    public record Outbox(
            boolean scheduleEnabled,
            int maxAttempts,
            int backoffSeconds,
            String marketingDestination,
            String vendorDestination,
            String forceFailureCode
    ) {
        public Outbox {
            if (maxAttempts <= 0) {
                maxAttempts = 5;
            }
            if (backoffSeconds <= 0) {
                backoffSeconds = 30;
            }
            if (marketingDestination == null || marketingDestination.isBlank()) {
                marketingDestination = "poc-mailbox";
            }
            if (vendorDestination == null || vendorDestination.isBlank()) {
                vendorDestination = "poc-vendor-stub";
            }
            if (forceFailureCode == null) {
                forceFailureCode = "";
            }
        }
    }
}
