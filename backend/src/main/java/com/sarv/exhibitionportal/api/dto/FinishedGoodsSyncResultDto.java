package com.sarv.exhibitionportal.api.dto;

public record FinishedGoodsSyncResultDto(
        String state,
        int rowsUpserted,
        int rowsDeactivated,
        String message
) {}
