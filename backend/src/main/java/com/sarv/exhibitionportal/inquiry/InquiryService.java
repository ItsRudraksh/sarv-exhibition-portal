package com.sarv.exhibitionportal.inquiry;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.outbox.OutboxService;
import com.sarv.exhibitionportal.review.ReviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InquiryService {

    private final InquiryRepository inquiries;
    private final ExhibitionProperties properties;
    private final AuditService audits;
    private final ReviewRepository reviews;
    private final OutboxService outbox;

    public InquiryService(
            InquiryRepository inquiries,
            ExhibitionProperties properties,
            AuditService audits,
            ReviewRepository reviews,
            OutboxService outbox
    ) {
        this.inquiries = inquiries;
        this.properties = properties;
        this.audits = audits;
        this.reviews = reviews;
        this.outbox = outbox;
    }

    @Transactional
    public InquiryDraftDto create(InquiryDraftDto requested) {
        UUID id = requested != null && requested.id() != null ? requested.id() : UUID.randomUUID();
        if (inquiries.exists(id)) {
            return inquiries.findDraft(id).orElseThrow();
        }
        UUID campaignId = properties.defaultCampaignId();
        UUID exhibitionId = inquiries.exhibitionIdForCampaign(campaignId)
                .orElseThrow(() -> new IllegalStateException("POC campaign seed is missing"));
        String channel = requested != null && requested.entryChannel() != null
                ? requested.entryChannel()
                : "EXHIBITION_QR";
        String reference = referenceCode(id);
        inquiries.insertDraft(id, reference, channel, campaignId, exhibitionId);
        inquiries.insertWorkflowEvent(id, "INQUIRY", null, "DRAFT");
        audits.record(id, "INQUIRY", id, "INQUIRY_CREATED", Map.of("lifecycle", "DRAFT"));
        if (requested != null) {
            InquiryDraftDto withId = withId(requested, id, "DRAFT", reference, null);
            inquiries.saveDraft(withId);
        }
        return inquiries.findDraft(id).orElseThrow();
    }

    @Transactional(readOnly = true)
    public InquiryDraftDto get(UUID id) {
        return inquiries.findDraft(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry not found"));
    }

    @Transactional
    public InquiryDraftDto save(UUID id, InquiryDraftDto incoming) {
        InquiryDraftDto existing = get(id);
        if ("SUBMITTED".equals(existing.lifecycleState())) {
            throw new InquiryValidationException("Submitted inquiries cannot be changed.");
        }
        InquiryDraftDto toSave = withId(
                incoming == null ? existing : incoming,
                id,
                "DRAFT",
                existing.referenceCode(),
                null
        );
        if (toSave.contactConfirmed()) {
            InquiryRules.assertContact(toSave.contact());
        }
        inquiries.saveDraft(toSave);
        return inquiries.findDraft(id).orElseThrow();
    }

    @Transactional
    public InquiryDraftDto confirmContact(UUID id, InquiryDraftDto incoming) {
        InquiryDraftDto merged = save(id, incoming);
        InquiryRules.assertContact(merged.contact());
        InquiryDraftDto confirmed = new InquiryDraftDto(
                merged.id(),
                merged.lifecycleState(),
                "intent-selection",
                merged.route(),
                merged.entryChannel(),
                merged.cardFront(),
                merged.cardBack(),
                merged.cardQrPayloadInternal(),
                merged.contact(),
                merged.supplier(),
                merged.departmentIds(),
                merged.productTypeIds(),
                merged.buyer(),
                true,
                merged.submittedAt(),
                merged.referenceCode()
        );
        inquiries.saveDraft(confirmed);
        audits.record(id, "INQUIRY", id, "INQUIRY_UPDATED", Map.of("step", "contact-confirm"));
        return inquiries.findDraft(id).orElseThrow();
    }

    @Transactional
    public InquiryDraftDto submit(UUID id, InquiryDraftDto incoming) {
        InquiryDraftDto existing = get(id);
        if ("SUBMITTED".equals(existing.lifecycleState())) {
            return existing;
        }
        InquiryDraftDto merged = withId(
                incoming == null ? existing : incoming,
                id,
                "DRAFT",
                existing.referenceCode(),
                null
        );
        InquiryRules.assertCanSubmit(merged);
        Instant submittedAt = Instant.now();
        String confirmationStep = "SUPPLIER".equals(merged.route())
                ? "supplier-confirmation"
                : "buyer-confirmation";
        InquiryDraftDto submitted = new InquiryDraftDto(
                merged.id(),
                "SUBMITTED",
                confirmationStep,
                merged.route(),
                merged.entryChannel(),
                merged.cardFront(),
                merged.cardBack(),
                merged.cardQrPayloadInternal(),
                merged.contact(),
                merged.supplier(),
                merged.departmentIds(),
                merged.productTypeIds(),
                merged.buyer(),
                true,
                submittedAt,
                merged.referenceCode()
        );
        inquiries.saveDraft(submitted);
        inquiries.insertWorkflowEvent(id, "INQUIRY", "DRAFT", "SUBMITTED");
        if ("SUPPLIER".equals(merged.route())) {
            inquiries.markSupplierReviewSubmitted(id);
            inquiries.insertWorkflowEvent(id, "SUPPLIER_REVIEW", "DRAFT", "SUBMITTED");
            reviews.openCaseIfAbsent(id);
        } else {
            inquiries.markPurchaseLeadSubmitted(id);
            inquiries.insertWorkflowEvent(id, "PURCHASE_LEAD", "DRAFT", "SUBMITTED");
            outbox.enqueueMarketingLead(id);
        }
        audits.record(id, "INQUIRY", id, "INQUIRY_SUBMITTED", Map.of(
                "lifecycle", "SUBMITTED",
                "route", merged.route()
        ));
        return inquiries.findDraft(id).orElseThrow();
    }

    static InquiryDraftDto emptyDraft(UUID id, String referenceCode) {
        return new InquiryDraftDto(
                id,
                "DRAFT",
                "card-capture",
                null,
                "EXHIBITION_QR",
                null,
                null,
                null,
                new ContactDto("", "", "+91", ""),
                new SupplierDto("", "", "", "", null),
                List.of(),
                List.of(),
                new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", "")),
                false,
                null,
                referenceCode
        );
    }

    private static InquiryDraftDto withId(
            InquiryDraftDto source,
            UUID id,
            String lifecycle,
            String referenceCode,
            Instant submittedAt
    ) {
        InquiryDraftDto base = source == null ? emptyDraft(id, referenceCode) : source;
        return new InquiryDraftDto(
                id,
                lifecycle,
                base.currentStep() == null ? "card-capture" : base.currentStep(),
                blankToNull(base.route()),
                base.entryChannel() == null ? "EXHIBITION_QR" : base.entryChannel(),
                base.cardFront(),
                base.cardBack(),
                base.cardQrPayloadInternal(),
                base.contact() == null ? new ContactDto("", "", "+91", "") : base.contact(),
                base.supplier() == null ? new SupplierDto("", "", "", "", null) : base.supplier(),
                base.departmentIds() == null ? List.of() : base.departmentIds(),
                base.productTypeIds() == null ? List.of() : base.productTypeIds(),
                base.buyer() == null
                        ? new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", ""))
                        : base.buyer(),
                base.contactConfirmed(),
                submittedAt,
                referenceCode
        );
    }

    private static String referenceCode(UUID id) {
        return "POC-" + id.toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
