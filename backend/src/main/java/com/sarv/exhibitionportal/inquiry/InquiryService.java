package com.sarv.exhibitionportal.inquiry;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.CampaignDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.CreateInquiryRequest;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.campaign.CampaignRepository;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.extraction.ExtractionService;
import com.sarv.exhibitionportal.outbox.OutboxService;
import com.sarv.exhibitionportal.review.ReviewRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InquiryService {

    private static final Set<String> CHANNELS = Set.of("EXHIBITION_QR", "WEBSITE", "DIRECT");

    private final InquiryRepository inquiries;
    private final CampaignRepository campaigns;
    private final ExhibitionProperties properties;
    private final AuditService audits;
    private final ReviewRepository reviews;
    private final OutboxService outbox;
    private final ExtractionService extractions;

    public InquiryService(
            InquiryRepository inquiries,
            CampaignRepository campaigns,
            ExhibitionProperties properties,
            AuditService audits,
            ReviewRepository reviews,
            OutboxService outbox,
            ExtractionService extractions
    ) {
        this.inquiries = inquiries;
        this.campaigns = campaigns;
        this.properties = properties;
        this.audits = audits;
        this.reviews = reviews;
        this.outbox = outbox;
        this.extractions = extractions;
    }

    @Transactional
    public InquiryDraftDto create(CreateInquiryRequest requested) {
        UUID id = requested != null && requested.id() != null ? requested.id() : UUID.randomUUID();
        if (inquiries.exists(id)) {
            return redactQr(inquiries.findDraft(id).orElseThrow());
        }

        String channel = resolveChannel(requested);
        UUID campaignId = null;
        UUID exhibitionId = null;
        String campaignCode = null;
        if ("EXHIBITION_QR".equals(channel)) {
            CampaignDto campaign = resolveExhibitionCampaign(requested);
            campaignId = campaign.id();
            exhibitionId = campaign.exhibitionId();
            campaignCode = campaign.code();
        } else if (requested != null && requested.campaignCode() != null && !requested.campaignCode().isBlank()) {
            throw new InquiryValidationException(
                    "campaignCode is only valid for exhibition QR entry.");
        }

        boolean staffAssisted = requested != null && Boolean.TRUE.equals(requested.staffAssisted());
        String reference = referenceCode(id);
        inquiries.insertDraft(id, reference, channel, campaignId, exhibitionId);
        inquiries.insertWorkflowEvent(id, "INQUIRY", null, "DRAFT");
        Map<String, Object> meta = new HashMap<>();
        meta.put("lifecycle", "DRAFT");
        meta.put("entryChannel", channel);
        if (campaignCode != null) {
            meta.put("campaignCode", campaignCode);
        }
        meta.put("staffAssisted", staffAssisted);
        audits.record(id, "INQUIRY", id, "INQUIRY_CREATED", meta);
        return redactQr(inquiries.findDraft(id).orElseThrow());
    }

    private String resolveChannel(CreateInquiryRequest requested) {
        String channel = requested != null && requested.entryChannel() != null
                ? requested.entryChannel().trim().toUpperCase(Locale.ROOT)
                : "EXHIBITION_QR";
        if (!CHANNELS.contains(channel)) {
            throw new InquiryValidationException("entryChannel must be EXHIBITION_QR, WEBSITE, or DIRECT.");
        }
        return channel;
    }

    private CampaignDto resolveExhibitionCampaign(CreateInquiryRequest requested) {
        if (requested != null && requested.campaignCode() != null && !requested.campaignCode().isBlank()) {
            CampaignDto campaign = campaigns.findByCode(requested.campaignCode().trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
            if (!campaign.active()) {
                throw new InquiryValidationException("That campaign QR is not active.");
            }
            return campaign;
        }
        UUID defaultId = properties.defaultCampaignId();
        return campaigns.findById(defaultId)
                .orElseThrow(() -> new IllegalStateException("POC campaign seed is missing"));
    }

    @Transactional(readOnly = true)
    public InquiryDraftDto get(UUID id) {
        return redactQr(inquiries.findDraft(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry not found")));
    }

    @Transactional
    public InquiryDraftDto save(UUID id, InquiryDraftDto incoming) {
        InquiryDraftDto existing = inquiries.findDraft(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry not found"));
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
        return redactQr(inquiries.findDraft(id).orElseThrow());
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
                null,
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
        extractions.reviewAgainstConfirmedContact(id, confirmed.contact(), confirmed.supplier());
        audits.record(id, "INQUIRY", id, "INQUIRY_UPDATED", Map.of("step", "contact-confirm"));
        return redactQr(inquiries.findDraft(id).orElseThrow());
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
                null,
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
        return redactQr(inquiries.findDraft(id).orElseThrow());
    }

    /** Card QR payloads are internal-only — never serialize them to the visitor API. */
    static InquiryDraftDto redactQr(InquiryDraftDto draft) {
        if (draft == null || draft.cardQrPayloadInternal() == null) {
            return draft;
        }
        return new InquiryDraftDto(
                draft.id(),
                draft.lifecycleState(),
                draft.currentStep(),
                draft.route(),
                draft.entryChannel(),
                draft.cardFront(),
                draft.cardBack(),
                null,
                draft.contact(),
                draft.supplier(),
                draft.departmentIds(),
                draft.productTypeIds(),
                draft.buyer(),
                draft.contactConfirmed(),
                draft.submittedAt(),
                draft.referenceCode()
        );
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
                null,
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

    private String referenceCode(UUID id) {
        String prefix = properties.referencePrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = properties.poc() ? "POC-" : "EP-";
        }
        return prefix + id.toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
