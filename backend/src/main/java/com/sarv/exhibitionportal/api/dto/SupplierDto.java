package com.sarv.exhibitionportal.api.dto;

import java.util.List;

public record SupplierDto(
        String companyName,
        String websiteUrl,
        String jobTitle,
        String locationFromCard,
        CardFileDto catalogueFile,
        boolean otherCategory,
        boolean otherProductType,
        String otherCategoryDetail,
        String otherProductTypeDetail,
        String capabilityNotes,
        List<CardFileDto> attachments
) {
    public SupplierDto {
        if (otherCategoryDetail == null) {
            otherCategoryDetail = "";
        }
        if (otherProductTypeDetail == null) {
            otherProductTypeDetail = "";
        }
        if (capabilityNotes == null) {
            capabilityNotes = "";
        }
        if (attachments == null) {
            attachments = List.of();
        }
    }

    public SupplierDto(
            String companyName,
            String websiteUrl,
            String jobTitle,
            String locationFromCard,
            CardFileDto catalogueFile
    ) {
        this(companyName, websiteUrl, jobTitle, locationFromCard, catalogueFile, false, false, "", "", "", List.of());
    }

    public SupplierDto(
            String companyName,
            String websiteUrl,
            String jobTitle,
            String locationFromCard,
            CardFileDto catalogueFile,
            String capabilityNotes
    ) {
        this(
                companyName,
                websiteUrl,
                jobTitle,
                locationFromCard,
                catalogueFile,
                false,
                false,
                "",
                "",
                capabilityNotes,
                List.of());
    }

    /** Legacy 9-arg shape: Other flags + notes, empty Other details. */
    public SupplierDto(
            String companyName,
            String websiteUrl,
            String jobTitle,
            String locationFromCard,
            CardFileDto catalogueFile,
            boolean otherCategory,
            boolean otherProductType,
            String capabilityNotes,
            List<CardFileDto> attachments
    ) {
        this(
                companyName,
                websiteUrl,
                jobTitle,
                locationFromCard,
                catalogueFile,
                otherCategory,
                otherProductType,
                "",
                "",
                capabilityNotes,
                attachments);
    }
}
