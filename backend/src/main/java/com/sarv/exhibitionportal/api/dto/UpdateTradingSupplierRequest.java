package com.sarv.exhibitionportal.api.dto;

/** Rename or notes for a trading supplier. */
public record UpdateTradingSupplierRequest(
        String companyName,
        String contactName,
        String email,
        String phone,
        String websiteUrl,
        String notes,
        String status
) {}
