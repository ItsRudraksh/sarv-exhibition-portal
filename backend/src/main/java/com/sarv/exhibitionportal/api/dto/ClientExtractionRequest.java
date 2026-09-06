package com.sarv.exhibitionportal.api.dto;

import java.util.List;
import java.util.UUID;

public record ClientExtractionRequest(
        String feature,
        UUID assetId,
        String providerModelReference,
        List<ProposedFieldInput> fields
) {
    public record ProposedFieldInput(String fieldKey, String proposedValueText) {}
}
