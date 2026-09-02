package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

public record FileAssetDto(
        UUID id,
        UUID inquiryId,
        String purpose,
        String originalFilename,
        String mediaType,
        long byteSize,
        String securityScanState,
        String processingState
) {}
