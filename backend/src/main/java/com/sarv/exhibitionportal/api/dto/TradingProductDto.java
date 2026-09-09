package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

public record TradingProductDto(
        UUID id,
        String name,
        boolean listedForBuyers,
        boolean active
) {}
