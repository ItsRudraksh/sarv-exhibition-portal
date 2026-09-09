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
        String capabilityNotes,
        List<CardFileDto> attachments
) {
    public SupplierDto {
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
        this(companyName, websiteUrl, jobTitle, locationFromCard, catalogueFile, false, false, "", List.of());
    }

    public SupplierDto(
            String companyName,
            String websiteUrl,
            String jobTitle,
            String locationFromCard,
            CardFileDto catalogueFile,
            String capabilityNotes
    ) {
        this(companyName, websiteUrl, jobTitle, locationFromCard, catalogueFile, false, false, capabilityNotes, List.of());
    }
}
