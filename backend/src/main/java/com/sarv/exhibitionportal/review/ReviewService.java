package com.sarv.exhibitionportal.review;

import com.sarv.exhibitionportal.api.dto.BuyerLeadDto;
import com.sarv.exhibitionportal.api.dto.SupplierReviewDto;
import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.inquiry.InquiryRepository;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import com.sarv.exhibitionportal.outbox.OutboxService;
import com.sarv.exhibitionportal.staff.StaffUser;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final InquiryRepository inquiries;
    private final AuditService audits;
    private final OutboxService outbox;

    public ReviewService(
            ReviewRepository reviews,
            InquiryRepository inquiries,
            AuditService audits,
            OutboxService outbox
    ) {
        this.reviews = reviews;
        this.inquiries = inquiries;
        this.audits = audits;
        this.outbox = outbox;
    }

    @Transactional(readOnly = true)
    public List<SupplierReviewDto> suppliers() {
        return reviews.listSuppliers().stream().map(this::toSupplier).toList();
    }

    @Transactional(readOnly = true)
    public SupplierReviewDto supplier(UUID id) {
        return reviews.findSupplier(id)
                .map(this::toSupplier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier inquiry not found"));
    }

    @Transactional(readOnly = true)
    public List<BuyerLeadDto> buyers() {
        return reviews.listBuyers().stream().map(this::toBuyer).toList();
    }

    @Transactional
    public BuyerLeadDto saveBuyerNotes(UUID id, String notes, StaffUser actor) {
        if (!reviews.buyerExists(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer inquiry not found");
        }
        reviews.updateMarketingNotes(id, notes);
        audits.recordUser(id, "PURCHASE_INQUIRY", id, "BUYER_NOTES_UPDATED", actor.id(), Map.of());
        return buyers().stream()
                .filter(row -> row.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    @Transactional
    public SupplierReviewDto decide(UUID inquiryId, String decision, String notes, StaffUser actor) {
        ReviewRepository.SupplierReviewRow current = reviews.findSupplier(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier inquiry not found"));
        UUID caseId = reviews.openCaseId(inquiryId)
                .orElseThrow(() -> new InquiryValidationException("No open review case for this supplier."));
        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        return switch (normalized) {
            case "APPROVE" -> addToProduction(current, caseId, notes, actor);
            case "REJECT" -> reject(current, caseId, notes, actor);
            case "REQUEST_INFORMATION" -> requestInformation(current, caseId, notes, actor);
            default -> throw new InquiryValidationException("Unknown review decision.");
        };
    }

    private SupplierReviewDto addToProduction(
            ReviewRepository.SupplierReviewRow current,
            UUID caseId,
            String notes,
            StaffUser actor
    ) {
        if ("APPROVED".equals(current.reviewState()) || !"NOT_REQUESTED".equals(current.productionState())) {
            throw new InquiryValidationException("Add to production has already been recorded for this supplier.");
        }
        if ("REJECTED".equals(current.reviewState())) {
            throw new InquiryValidationException("A rejected supplier cannot be added to production.");
        }
        reviews.insertDecision(caseId, "APPROVE", notes, actor.id());
        reviews.applyApprove(current.id(), actor.id());
        reviews.closeCase(caseId);
        inquiries.insertWorkflowEvent(
                current.id(), "SUPPLIER_REVIEW", current.reviewState(), "APPROVED", "USER", actor.id());
        inquiries.insertWorkflowEvent(
                current.id(), "VENDOR_DELIVERY", "NOT_REQUESTED", "QUEUED", "USER", actor.id());
        outbox.enqueueVendorUpsert(current.id());
        audits.recordUser(
                current.id(),
                "SUPPLIER_INQUIRY",
                current.id(),
                "ADD_TO_PRODUCTION",
                actor.id(),
                Map.of("productionState", "QUEUED")
        );
        return supplier(current.id());
    }

    private SupplierReviewDto reject(
            ReviewRepository.SupplierReviewRow current,
            UUID caseId,
            String notes,
            StaffUser actor
    ) {
        if ("APPROVED".equals(current.reviewState())) {
            throw new InquiryValidationException("An approved supplier cannot be rejected in this POC.");
        }
        reviews.insertDecision(caseId, "REJECT", notes, actor.id());
        reviews.applyReject(current.id());
        reviews.closeCase(caseId);
        inquiries.insertWorkflowEvent(
                current.id(), "SUPPLIER_REVIEW", current.reviewState(), "REJECTED", "USER", actor.id());
        audits.recordUser(
                current.id(),
                "SUPPLIER_INQUIRY",
                current.id(),
                "SUPPLIER_REJECTED",
                actor.id(),
                Map.of("productionState", "NOT_REQUESTED")
        );
        return supplier(current.id());
    }

    private SupplierReviewDto requestInformation(
            ReviewRepository.SupplierReviewRow current,
            UUID caseId,
            String notes,
            StaffUser actor
    ) {
        if ("APPROVED".equals(current.reviewState()) || "REJECTED".equals(current.reviewState())) {
            throw new InquiryValidationException("Closed reviews cannot request more information.");
        }
        reviews.insertDecision(caseId, "REQUEST_INFORMATION", notes, actor.id());
        reviews.applyNeedsInformation(current.id());
        reviews.waitingForInfo(caseId);
        inquiries.insertWorkflowEvent(
                current.id(), "SUPPLIER_REVIEW", current.reviewState(), "NEEDS_INFORMATION", "USER", actor.id());
        audits.recordUser(
                current.id(),
                "SUPPLIER_INQUIRY",
                current.id(),
                "SUPPLIER_NEEDS_INFORMATION",
                actor.id(),
                Map.of("productionState", "NOT_REQUESTED")
        );
        return supplier(current.id());
    }

    private SupplierReviewDto toSupplier(ReviewRepository.SupplierReviewRow row) {
        return new SupplierReviewDto(
                row.id(),
                row.referenceCode(),
                row.submittedAt(),
                row.reviewState(),
                row.productionState(),
                row.websiteUrl(),
                row.approvedAt(),
                row.approvedByUserId(),
                row.companyName(),
                row.personName(),
                row.email(),
                row.phone(),
                outbox.latestState(row.id(), OutboxService.VENDOR_UPSERT)
        );
    }

    private BuyerLeadDto toBuyer(ReviewRepository.BuyerLeadRow row) {
        return new BuyerLeadDto(
                row.id(),
                row.referenceCode(),
                row.submittedAt(),
                row.leadState(),
                row.marketingNotes(),
                row.companyName(),
                row.personName(),
                row.email(),
                row.phone(),
                row.requirement(),
                outbox.latestState(row.id(), OutboxService.MARKETING_LEAD)
        );
    }
}
