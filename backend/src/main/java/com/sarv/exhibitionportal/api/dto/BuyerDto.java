package com.sarv.exhibitionportal.api.dto;

public record BuyerDto(
        String requirement,
        String productAreaSearch,
        BuyerSpecificationsDto specifications
) {}
