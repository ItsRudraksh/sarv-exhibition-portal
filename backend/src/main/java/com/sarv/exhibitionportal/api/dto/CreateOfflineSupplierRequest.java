package com.sarv.exhibitionportal.api.dto;

public record CreateOfflineSupplierRequest(
        String companyName,
        String contactName,
        String email,
        String phone,
        String websiteUrl,
        String notes
) {}
