package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.ExtractionDto;
import com.sarv.exhibitionportal.extraction.ExtractionService;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import java.util.Map;
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
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String feature = body == null || body.get("feature") == null
                ? "BUSINESS_CARD_SCAN"
                : String.valueOf(body.get("feature"));
        UUID assetId = null;
        if (body != null && body.get("assetId") != null) {
            try {
                assetId = UUID.fromString(String.valueOf(body.get("assetId")));
            } catch (IllegalArgumentException ex) {
                throw new InquiryValidationException("assetId must be a UUID.");
            }
        }
        return extractions.start(inquiryId, feature, assetId);
    }
}
