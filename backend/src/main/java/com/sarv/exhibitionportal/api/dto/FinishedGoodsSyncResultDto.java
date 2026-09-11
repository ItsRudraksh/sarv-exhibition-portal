package com.sarv.exhibitionportal.api.dto;

/** Staff sync summary (upserted / deactivated counts). */
public record FinishedGoodsSyncResultDto(
        String state,
        int rowsUpserted,
        int rowsDeactivated,
        String message
) {}
