package com.sarv.exhibitionportal.api.dto;

/** Admin: create a trading supplier that is not a portal sell inquiry. */
public record CreateOfflineSupplierRequest(
        String companyName,
        String contactName,
        String email,
        String phone,
        String websiteUrl,
        String notes
) {}
