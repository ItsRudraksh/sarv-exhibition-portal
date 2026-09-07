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
        String staffBootstrapPassword,
        String referencePrefix,
        PharmaErp pharmaErp
) {
    public ExhibitionProperties {
        if (staffBootstrapPassword == null) {
            staffBootstrapPassword = "";
        }
        if (referencePrefix == null || referencePrefix.isBlank()) {
            referencePrefix = poc ? "POC-" : "EP-";
        }
        if (pharmaErp == null) {
            pharmaErp = new PharmaErp(false, "", "", "", false, "0 0 2 * * MON");
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
                marketingDestination = "local-mailbox";
            }
            if (vendorDestination == null || vendorDestination.isBlank()) {
                vendorDestination = "local-vendor-stub";
            }
            if (forceFailureCode == null) {
                forceFailureCode = "";
            }
        }
    }

    /**
     * Read-only sync from pharma-erp MySQL ({@code pharmadb.products} / OUTPUT_PRODUCTS).
     * Manual first ({@code schedule-enabled=false}); weekly cron when enabled.
     */
    public record PharmaErp(
            boolean enabled,
            String jdbcUrl,
            String username,
            String password,
            boolean scheduleEnabled,
            String syncCron
    ) {
        public PharmaErp {
            if (jdbcUrl == null) {
                jdbcUrl = "";
            }
            if (username == null) {
                username = "";
            }
            if (password == null) {
                password = "";
            }
            if (syncCron == null || syncCron.isBlank()) {
                syncCron = "0 0 2 * * MON";
            }
        }
    }
}
