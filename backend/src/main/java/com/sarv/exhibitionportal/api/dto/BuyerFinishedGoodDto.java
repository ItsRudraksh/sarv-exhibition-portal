package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

public record BuyerFinishedGoodDto(
        UUID finishedGoodId,
        String quantity
) {
    public BuyerFinishedGoodDto {
        if (quantity == null) {
            quantity = "";
        }
    }
}
