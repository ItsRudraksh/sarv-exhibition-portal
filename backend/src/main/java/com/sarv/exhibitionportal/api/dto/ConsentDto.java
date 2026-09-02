package com.sarv.exhibitionportal.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ConsentDto(
        UUID id,
        String purpose,
        String policyVersion,
        String decision,
        Instant decidedAt
) {}
