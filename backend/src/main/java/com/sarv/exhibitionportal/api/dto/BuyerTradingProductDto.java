package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Selected tagged trading product plus required quantity. */
public record BuyerTradingProductDto(UUID tradingProductId, String quantity) {
    public BuyerTradingProductDto {
        if (quantity == null) {
            quantity = "";
        }
    }
}
