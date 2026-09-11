package com.sarv.exhibitionportal.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Staff marketing-queue row for a submitted purchase inquiry. */
public record BuyerLeadDto(
        UUID id,
        String referenceCode,
        Instant submittedAt,
        String leadState,
        String marketingNotes,
        String companyName,
        String personName,
        String email,
        String phone,
        String requirement,
        String deliveryState
) {}
