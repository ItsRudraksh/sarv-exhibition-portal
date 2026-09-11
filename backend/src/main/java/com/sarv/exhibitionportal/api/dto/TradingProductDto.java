package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Product under an offline or portal-linked trading supplier. */
public record TradingProductDto(
        UUID id,
        String name,
        boolean listedForBuyers,
        boolean active
) {}
