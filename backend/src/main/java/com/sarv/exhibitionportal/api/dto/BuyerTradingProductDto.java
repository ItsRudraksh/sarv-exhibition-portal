package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

public record BuyerTradingProductDto(UUID tradingProductId, String quantity) {
    public BuyerTradingProductDto {
        if (quantity == null) {
            quantity = "";
        }
    }
}
