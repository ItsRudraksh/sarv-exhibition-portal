package com.sarv.exhibitionportal.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Excel export job status for buyer leads. */
public record ExportJobDto(
        UUID id,
        String scope,
        String state,
        String originalFilename,
        String mediaType,
        Long byteSize,
        Instant expiresAt,
        Instant generatedAt,
        String failureReason
) {}
