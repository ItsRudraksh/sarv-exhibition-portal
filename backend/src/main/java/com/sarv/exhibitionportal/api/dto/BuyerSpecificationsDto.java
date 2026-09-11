package com.sarv.exhibitionportal.api.dto;

/** Optional buyer specs. Standards are IP/USP/BP/EP only (no PP). */
public record BuyerSpecificationsDto(
        String quantity,
        String packSize,
        String standard,
        String neededByDate,
        String notes
) {}
