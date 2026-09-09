package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

public record PortalSupplierCandidateDto(
        UUID inquiryId,
        String referenceCode,
        String companyName,
        String personName,
        String reviewState
) {}
