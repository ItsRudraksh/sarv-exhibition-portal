package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Stored file metadata. Bytes live on disk, not in MySQL. */
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
