package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Submitted sell inquiry that can be linked as a trading supplier. */
public record PortalSupplierCandidateDto(
        UUID inquiryId,
        String referenceCode,
        String companyName,
        String personName,
        String reviewState
) {}
