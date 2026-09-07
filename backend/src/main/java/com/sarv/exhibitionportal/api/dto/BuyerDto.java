package com.sarv.exhibitionportal.api.dto;

import java.util.List;

public record BuyerDto(
        String requirement,
        String productAreaSearch,
        BuyerSpecificationsDto specifications,
        List<BuyerFinishedGoodDto> finishedGoods
) {
    public BuyerDto {
        if (finishedGoods == null) {
            finishedGoods = List.of();
        }
    }

    /** Convenience for callers that do not select finished goods. */
    public BuyerDto(String requirement, String productAreaSearch, BuyerSpecificationsDto specifications) {
        this(requirement, productAreaSearch, specifications, List.of());
    }
}
