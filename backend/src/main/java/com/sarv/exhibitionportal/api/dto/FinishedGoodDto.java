package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Pharma-erp snapshot row. Buyer UI shows name only. */
public record FinishedGoodDto(UUID id, Long externalId, String code, String name) {}
