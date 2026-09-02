package com.sarv.exhibitionportal.api.dto;

public record BuyerSpecificationsDto(
        String quantity,
        String packSize,
        String standard,
        String neededByDate,
        String notes
) {}
