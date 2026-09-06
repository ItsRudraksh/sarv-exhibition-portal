package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.ClientExtractionRequest;
import com.sarv.exhibitionportal.api.dto.ExtractionDto;
import com.sarv.exhibitionportal.extraction.CardScanResult;
import com.sarv.exhibitionportal.extraction.ExtractionService;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/inquiries/{inquiryId}/extractions")
public class ExtractionController {

    private static final BigDecimal CLIENT_OCR_CONFIDENCE = new BigDecimal("0.650");

    private final ExtractionService extractions;

    public ExtractionController(ExtractionService extractions) {
        this.extractions = extractions;
    }

    @GetMapping("/latest")
    public ExtractionDto latest(@PathVariable UUID inquiryId) {
        return extractions.latest(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No extraction yet"));
    }

    @PostMapping
    public ExtractionDto start(
            @PathVariable UUID inquiryId,
            @RequestBody(required = false) ClientExtractionRequest body
    ) {
        String feature = body == null || body.feature() == null || body.feature().isBlank()
                ? "BUSINESS_CARD_SCAN"
                : body.feature().trim();
        UUID assetId = body == null ? null : body.assetId();
        if ("CLIENT_CARD_OCR".equals(feature)) {
            if (assetId == null) {
                throw new InquiryValidationException("assetId is required for card scan.");
            }
            List<CardScanResult.ProposedField> fields = new ArrayList<>();
            if (body.fields() != null) {
                for (ClientExtractionRequest.ProposedFieldInput input : body.fields()) {
                    if (input == null || input.fieldKey() == null || input.proposedValueText() == null) {
                        continue;
                    }
                    fields.add(new CardScanResult.ProposedField(
                            input.fieldKey().trim(),
                            input.proposedValueText().trim(),
                            CLIENT_OCR_CONFIDENCE));
                }
            }
            return extractions.startClientOcr(
                    inquiryId, assetId, body.providerModelReference(), fields);
        }
        return extractions.start(inquiryId, feature, assetId);
    }
}
