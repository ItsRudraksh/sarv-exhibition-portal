package com.sarv.exhibitionportal.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExtractionDto(
        UUID id,
        UUID sessionId,
        UUID inquiryId,
        String feature,
        String state,
        UUID inputAssetId,
        String providerModelReference,
        boolean cardQrDetected,
        Instant completedAt,
        List<ExtractedFieldDto> fields
) {
    public record ExtractedFieldDto(
            UUID id,
            String fieldKey,
            String proposedValueText,
            BigDecimal confidenceScore,
            String reviewState
    ) {}
}
