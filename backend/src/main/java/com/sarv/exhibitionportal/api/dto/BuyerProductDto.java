package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Visitor-facing catalogue row. Names only — no item codes. */
public record BuyerProductDto(UUID id, String name, String sourceKind) {}
