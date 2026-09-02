package com.sarv.exhibitionportal.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SupplierReviewDto(
        UUID id,
        String referenceCode,
        Instant submittedAt,
        String reviewState,
        String productionState,
        String websiteUrl,
        Instant approvedAt,
        UUID approvedByUserId,
        String companyName,
        String personName,
        String email,
        String phone,
        String deliveryState
) {}
