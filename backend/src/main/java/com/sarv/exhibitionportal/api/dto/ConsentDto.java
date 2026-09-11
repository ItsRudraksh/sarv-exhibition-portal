package com.sarv.exhibitionportal.api.dto;

import java.time.Instant;
import java.util.UUID;

/** One consent decision. History is append-only; latest row wins. */
public record ConsentDto(
        UUID id,
        String purpose,
        String policyVersion,
        String decision,
        Instant decidedAt
) {}
