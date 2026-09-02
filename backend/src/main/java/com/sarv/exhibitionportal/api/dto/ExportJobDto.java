package com.sarv.exhibitionportal.api.dto;

import java.time.Instant;
import java.util.UUID;

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
