package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Stall QR campaign. Exhibition entry requires an active campaign. */
public record CampaignDto(
        UUID id,
        String code,
        String label,
        String landingRoute,
        UUID exhibitionId,
        boolean active
) {}
