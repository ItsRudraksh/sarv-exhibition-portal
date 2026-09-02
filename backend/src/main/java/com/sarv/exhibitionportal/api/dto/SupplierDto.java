package com.sarv.exhibitionportal.api.dto;

public record SupplierDto(
        String companyName,
        String websiteUrl,
        String jobTitle,
        String locationFromCard,
        CardFileDto catalogueFile
) {}
