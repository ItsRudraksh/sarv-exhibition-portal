package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

public record CampaignDto(
        UUID id,
        String code,
        String label,
        String landingRoute,
        UUID exhibitionId,
        boolean active
) {}
