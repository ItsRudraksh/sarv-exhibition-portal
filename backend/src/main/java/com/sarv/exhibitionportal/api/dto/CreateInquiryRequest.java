package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Body for POST /api/v1/inquiries. Extra InquiryDraft fields from older clients are ignored. */
public record CreateInquiryRequest(
        UUID id,
        String entryChannel,
        String campaignCode,
        Boolean staffAssisted
) {}
