package com.sarv.exhibitionportal.api.dto;

import java.util.List;
import java.util.UUID;

/** Admin catalogue supplier (OFFLINE or PORTAL_LINKED). */
public record TradingSupplierDto(
        UUID id,
        String sourceKind,
        UUID portalInquiryId,
        String companyName,
        String contactName,
        String email,
        String phone,
        String websiteUrl,
        String notes,
        String status,
        List<TradingProductDto> products,
        List<String> suggestedProductNames
) {
    public TradingSupplierDto {
        if (products == null) {
            products = List.of();
        }
        if (suggestedProductNames == null) {
            suggestedProductNames = List.of();
        }
    }
}
