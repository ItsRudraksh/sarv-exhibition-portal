package com.sarv.exhibitionportal.api.dto;

public record UpdateTradingSupplierRequest(
        String companyName,
        String contactName,
        String email,
        String phone,
        String websiteUrl,
        String notes,
        String status
) {}
