package com.sarv.exhibitionportal.extraction;

import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.ExtractionDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.consent.ConsentRepository;
import com.sarv.exhibitionportal.fileasset.FileAssetRepository;
import com.sarv.exhibitionportal.fileasset.LocalObjectStorage;
import com.sarv.exhibitionportal.inquiry.InquiryRepository;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExtractionService {

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "full_name",
            "work_email",
            "country_code",
            "mobile_number",
            "company_name",
            "job_title",
            "location_from_card"
    );

    private final ExtractionRepository extractions;
    private final ConsentRepository consents;
    private final InquiryRepository inquiries;
    private final FileAssetRepository files;
    private final LocalObjectStorage storage;
    private final LocalCardScanEngine scanner;
    private final AuditService audits;
    private final ExhibitionProperties properties;

    public ExtractionService(
            ExtractionRepository extractions,
            ConsentRepository consents,
            InquiryRepository inquiries,
            FileAssetRepository files,
            LocalObjectStorage storage,
            LocalCardScanEngine scanner,
            AuditService audits,
            ExhibitionProperties properties
    ) {
        this.extractions = extractions;
        this.consents = consents;
        this.inquiries = inquiries;
        this.files = files;
        this.storage = storage;
        this.scanner = scanner;
        this.audits = audits;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public Optional<ExtractionDto> latest(UUID inquiryId) {
        requireInquiry(inquiryId);
        return extractions.latestForInquiry(inquiryId);
    }

    @Transactional
    public ExtractionDto runCardScan(UUID inquiryId, UUID assetId) {
        InquiryDraftDto draft = inquiries.findDraft(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry not found"));
        if ("SUBMITTED".equals(draft.lifecycleState())) {
            throw new InquiryValidationException("Submitted inquiries cannot be changed.");
        }
        var consent = consents.latest(inquiryId, "BUSINESS_CARD_EXTRACTION")
                .filter(c -> "GRANTED".equals(c.decision()))
                .orElseThrow(() -> new InquiryValidationException(
                        "Card extraction requires granted BUSINESS_CARD_EXTRACTION consent."));
        var asset = files.find(inquiryId, assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (!"BUSINESS_CARD".equals(asset.purpose()) || !"CLEAN".equals(asset.securityScanState())) {
            throw new InquiryValidationException("Extraction needs a clean business-card image.");
        }

        Instant now = Instant.now();
        UUID sessionId = UUID.randomUUID();
        UUID extractionId = UUID.randomUUID();
        byte[] bytes;
        try {
            bytes = storage.read(Path.of(properties.storageRoot()), asset.storageKey());
        } catch (IOException ex) {
            extractions.insertSession(
                    sessionId, inquiryId, consent.id(), "BUSINESS_CARD_SCAN", null,
                    "FAILED", LocalCardScanEngine.PROVIDER, now);
            extractions.insertExtraction(
                    extractionId, sessionId, assetId, "FAILED", LocalCardScanEngine.PROVIDER, now);
            audits.record(inquiryId, "AI_EXTRACTION", extractionId, "CARD_EXTRACTION_FAILED", Map.of(
                    "reason", "read_failed"
            ));
            return extractions.find(inquiryId, extractionId).orElseThrow();
        }

        CardScanResult scan = scanner.scan(bytes);
        if (scan.qrPayloadInternal() != null && !scan.qrPayloadInternal().isBlank()) {
            inquiries.updateCardQrPayload(inquiryId, scan.qrPayloadInternal());
        }

        extractions.insertSession(
                sessionId, inquiryId, consent.id(), "BUSINESS_CARD_SCAN", null,
                "COMPLETED", LocalCardScanEngine.PROVIDER, now);
        extractions.insertExtraction(
                extractionId, sessionId, assetId, "COMPLETED", scan.providerModelReference(), now);

        Set<String> seen = new HashSet<>();
        for (CardScanResult.ProposedField field : scan.fields()) {
            if (!ALLOWED_KEYS.contains(field.fieldKey()) || seen.contains(field.fieldKey())) {
                continue;
            }
            seen.add(field.fieldKey());
            extractions.insertField(
                    UUID.randomUUID(),
                    extractionId,
                    field.fieldKey(),
                    field.value(),
                    field.confidence()
            );
        }

        audits.record(inquiryId, "AI_EXTRACTION", extractionId, "CARD_EXTRACTION_COMPLETED", Map.of(
                "qrDetected", scan.qrPayloadInternal() != null && !scan.qrPayloadInternal().isBlank(),
                "fieldCount", seen.size(),
                "provider", LocalCardScanEngine.PROVIDER
        ));
        return extractions.find(inquiryId, extractionId).orElseThrow();
    }

    @Transactional
    public ExtractionDto start(UUID inquiryId, String feature, UUID assetId) {
        if (feature == null || feature.isBlank()) {
            feature = "BUSINESS_CARD_SCAN";
        }
        if ("VOICE_INPUT".equals(feature)) {
            throw new InquiryValidationException(
                    "Voice assist is not enabled yet. Use the typed fields.");
        }
        if (!"BUSINESS_CARD_SCAN".equals(feature)) {
            throw new InquiryValidationException("Unknown assistive feature.");
        }
        if (assetId == null) {
            throw new InquiryValidationException("assetId is required for card scan.");
        }
        return runCardScan(inquiryId, assetId);
    }

    /**
     * After the visitor confirms contact, mark pending proposals ACCEPTED/CORRECTED/REJECTED.
     * Does not write contact fields — those were already saved by the visitor.
     */
    @Transactional
    public void reviewAgainstConfirmedContact(UUID inquiryId, ContactDto contact, SupplierDto supplier) {
        List<ExtractionRepository.FieldRow> pending = extractions.pendingFieldsForInquiry(inquiryId);
        if (pending.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        Set<String> seenKeys = new HashSet<>();
        for (ExtractionRepository.FieldRow field : pending) {
            if (!seenKeys.add(field.fieldKey())) {
                extractions.markFieldReviewed(field.id(), "REJECTED", now);
                continue;
            }
            String confirmed = confirmedValue(field.fieldKey(), contact, supplier);
            String proposed = field.proposedValueText() == null ? "" : field.proposedValueText().trim();
            if (confirmed == null || confirmed.isBlank()) {
                extractions.markFieldReviewed(field.id(), "REJECTED", now);
            } else if (equalsIgnoreCase(proposed, confirmed)) {
                extractions.markFieldReviewed(field.id(), "ACCEPTED", now);
            } else {
                extractions.markFieldReviewed(field.id(), "CORRECTED", now);
            }
        }
    }

    private static String confirmedValue(String key, ContactDto contact, SupplierDto supplier) {
        return switch (key) {
            case "full_name" -> contact == null ? null : contact.fullName();
            case "work_email" -> contact == null ? null : contact.workEmail();
            case "country_code" -> contact == null ? null : contact.countryCode();
            case "mobile_number" -> contact == null ? null : contact.mobileNumber();
            case "company_name" -> supplier == null ? null : supplier.companyName();
            case "job_title" -> supplier == null ? null : supplier.jobTitle();
            case "location_from_card" -> supplier == null ? null : supplier.locationFromCard();
            default -> null;
        };
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return Objects.equals(
                a == null ? "" : a.trim().toLowerCase(Locale.ROOT),
                b == null ? "" : b.trim().toLowerCase(Locale.ROOT)
        );
    }

    private void requireInquiry(UUID inquiryId) {
        if (!inquiries.exists(inquiryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry not found");
        }
    }
}
