package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Selected pharma-erp finished good plus required quantity. */
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
