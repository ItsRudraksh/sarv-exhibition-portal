package com.sarv.exhibitionportal.api.dto;

import java.util.List;

public record BuyerDto(
        String requirement,
        String productAreaSearch,
        BuyerSpecificationsDto specifications,
        List<BuyerFinishedGoodDto> finishedGoods,
        List<BuyerTradingProductDto> tradingProducts,
        List<CardFileDto> attachments,
        boolean otherProduct,
        String otherProductDetail
) {
    public BuyerDto {
        if (finishedGoods == null) {
            finishedGoods = List.of();
        }
        if (tradingProducts == null) {
            tradingProducts = List.of();
        }
        if (attachments == null) {
            attachments = List.of();
        }
        if (otherProductDetail == null) {
            otherProductDetail = "";
        }
    }

    /** Convenience for callers that do not select catalogue rows. */
    public BuyerDto(String requirement, String productAreaSearch, BuyerSpecificationsDto specifications) {
        this(requirement, productAreaSearch, specifications, List.of(), List.of(), List.of(), false, "");
    }

    public BuyerDto(
            String requirement,
            String productAreaSearch,
            BuyerSpecificationsDto specifications,
            List<BuyerFinishedGoodDto> finishedGoods
    ) {
        this(requirement, productAreaSearch, specifications, finishedGoods, List.of(), List.of(), false, "");
    }

    public BuyerDto(
            String requirement,
            String productAreaSearch,
            BuyerSpecificationsDto specifications,
            List<BuyerFinishedGoodDto> finishedGoods,
            List<BuyerTradingProductDto> tradingProducts
    ) {
        this(requirement, productAreaSearch, specifications, finishedGoods, tradingProducts, List.of(), false, "");
    }

    public BuyerDto(
            String requirement,
            String productAreaSearch,
            BuyerSpecificationsDto specifications,
            List<BuyerFinishedGoodDto> finishedGoods,
            List<BuyerTradingProductDto> tradingProducts,
            List<CardFileDto> attachments
    ) {
        this(
                requirement,
                productAreaSearch,
                specifications,
                finishedGoods,
                tradingProducts,
                attachments,
                false,
                "");
    }
}
