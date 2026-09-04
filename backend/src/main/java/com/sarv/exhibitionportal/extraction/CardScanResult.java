package com.sarv.exhibitionportal.extraction;

import java.math.BigDecimal;
import java.util.List;

/** Result of a local assistive pass over a card image. Raw QR text is never returned to visitors. */
public record CardScanResult(
        String qrPayloadInternal,
        List<ProposedField> fields,
        String providerModelReference
) {
    public record ProposedField(String fieldKey, String value, BigDecimal confidence) {}

    public static CardScanResult empty(String provider) {
        return new CardScanResult(null, List.of(), provider);
    }
}
