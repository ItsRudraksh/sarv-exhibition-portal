package com.sarv.exhibitionportal.review;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ReviewRepository {

    private final JdbcClient jdbc;

    public ReviewRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void openCaseIfAbsent(UUID supplierInquiryId) {
        Long open = jdbc.sql("""
                select count(*) from review_cases
                where supplier_inquiry_id = :id and state <> 'CLOSED'
                """)
                .param("id", supplierInquiryId)
                .query(Long.class)
                .single();
        if (open != null && open > 0) {
            return;
        }
        jdbc.sql("""
                 insert into review_cases (id, supplier_inquiry_id, state)
                 values (:id, :sid, 'OPEN')
                 """)
                .param("id", UUID.randomUUID())
                .param("sid", supplierInquiryId)
                .update();
    }

    public Optional<UUID> openCaseId(UUID supplierInquiryId) {
        return jdbc.sql("""
                select id from review_cases
                where supplier_inquiry_id = :id and state <> 'CLOSED'
                order by opened_at desc
                limit 1
                """)
                .param("id", supplierInquiryId)
                .query(UUID.class)
                .optional();
    }

    public void insertDecision(UUID caseId, String decision, String notes, UUID actorId) {
        jdbc.sql("""
                 insert into review_decisions (
                     id, review_case_id, decision, notes, decided_by_user_id
                 ) values (
                     :id, :case, :decision, :notes, :actor
                 )
                 """)
                .param("id", UUID.randomUUID())
                .param("case", caseId)
                .param("decision", decision)
                .param("notes", notes)
                .param("actor", actorId)
                .update();
    }

    public void closeCase(UUID caseId) {
        jdbc.sql("""
                 update review_cases
                 set state = 'CLOSED', closed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                 where id = :id and state <> 'CLOSED'
                 """)
                .param("id", caseId)
                .update();
    }

    public void waitingForInfo(UUID caseId) {
        jdbc.sql("""
                 update review_cases
                 set state = 'WAITING_FOR_INFO', updated_at = CURRENT_TIMESTAMP
                 where id = :id and state <> 'CLOSED'
                 """)
                .param("id", caseId)
                .update();
    }

    public Optional<SupplierReviewRow> findSupplier(UUID inquiryId) {
        return jdbc.sql("""
                select i.id, i.reference_code, i.submitted_at, s.review_state, s.production_state,
                       s.website_url, s.approved_at, s.approved_by_user_id,
                       p.company_name_submitted, p.person_name_submitted, p.email_submitted, p.phone_submitted
                from inquiries i
                join supplier_inquiries s on s.inquiry_id = i.id
                left join inquiry_parties p on p.inquiry_id = i.id and p.role = 'SUPPLIER_CONTACT'
                where i.id = :id and i.lifecycle_state = 'SUBMITTED' and i.route = 'SUPPLIER'
                """)
                .param("id", inquiryId)
                .query((rs, n) -> new SupplierReviewRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("reference_code"),
                        ts(rs.getTimestamp("submitted_at")),
                        rs.getString("review_state"),
                        rs.getString("production_state"),
                        rs.getString("website_url"),
                        ts(rs.getTimestamp("approved_at")),
                        rs.getObject("approved_by_user_id", UUID.class),
                        rs.getString("company_name_submitted"),
                        rs.getString("person_name_submitted"),
                        rs.getString("email_submitted"),
                        rs.getString("phone_submitted")
                ))
                .optional();
    }

    public List<SupplierReviewRow> listSuppliers() {
        return jdbc.sql("""
                select i.id, i.reference_code, i.submitted_at, s.review_state, s.production_state,
                       s.website_url, s.approved_at, s.approved_by_user_id,
                       p.company_name_submitted, p.person_name_submitted, p.email_submitted, p.phone_submitted
                from inquiries i
                join supplier_inquiries s on s.inquiry_id = i.id
                left join inquiry_parties p on p.inquiry_id = i.id and p.role = 'SUPPLIER_CONTACT'
                where i.lifecycle_state = 'SUBMITTED' and i.route = 'SUPPLIER'
                order by i.submitted_at desc
                """)
                .query((rs, n) -> new SupplierReviewRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("reference_code"),
                        ts(rs.getTimestamp("submitted_at")),
                        rs.getString("review_state"),
                        rs.getString("production_state"),
                        rs.getString("website_url"),
                        ts(rs.getTimestamp("approved_at")),
                        rs.getObject("approved_by_user_id", UUID.class),
                        rs.getString("company_name_submitted"),
                        rs.getString("person_name_submitted"),
                        rs.getString("email_submitted"),
                        rs.getString("phone_submitted")
                ))
                .list();
    }

    public void applyApprove(UUID inquiryId, UUID actorId) {
        jdbc.sql("""
                 update supplier_inquiries
                 set review_state = 'APPROVED',
                     production_state = 'QUEUED',
                     approved_at = CURRENT_TIMESTAMP,
                     approved_by_user_id = :actor,
                     updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("id", inquiryId)
                .param("actor", actorId)
                .update();
    }

    public void applyReject(UUID inquiryId) {
        jdbc.sql("""
                 update supplier_inquiries
                 set review_state = 'REJECTED',
                     production_state = 'NOT_REQUESTED',
                     approved_at = null,
                     approved_by_user_id = null,
                     updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("id", inquiryId)
                .update();
    }

    public void applyNeedsInformation(UUID inquiryId) {
        jdbc.sql("""
                 update supplier_inquiries
                 set review_state = 'NEEDS_INFORMATION',
                     production_state = 'NOT_REQUESTED',
                     updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("id", inquiryId)
                .update();
    }

    public List<BuyerLeadRow> listBuyers() {
        return jdbc.sql("""
                select i.id, i.reference_code, i.submitted_at, pi.lead_state, pi.marketing_notes,
                       p.company_name_submitted, p.person_name_submitted, p.email_submitted, p.phone_submitted,
                       pli.requirement_text
                from inquiries i
                join purchase_inquiries pi on pi.inquiry_id = i.id
                left join inquiry_parties p on p.inquiry_id = i.id and p.role = 'BUYER_CONTACT'
                left join purchase_line_items pli on pli.purchase_inquiry_id = i.id
                where i.lifecycle_state = 'SUBMITTED' and i.route = 'PURCHASE'
                order by i.submitted_at desc
                """)
                .query((rs, n) -> new BuyerLeadRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("reference_code"),
                        ts(rs.getTimestamp("submitted_at")),
                        rs.getString("lead_state"),
                        rs.getString("marketing_notes"),
                        rs.getString("company_name_submitted"),
                        rs.getString("person_name_submitted"),
                        rs.getString("email_submitted"),
                        rs.getString("phone_submitted"),
                        rs.getString("requirement_text")
                ))
                .list();
    }

    public boolean buyerExists(UUID inquiryId) {
        Long count = jdbc.sql("""
                select count(*) from inquiries i
                join purchase_inquiries pi on pi.inquiry_id = i.id
                where i.id = :id and i.lifecycle_state = 'SUBMITTED' and i.route = 'PURCHASE'
                """)
                .param("id", inquiryId)
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    public void updateMarketingNotes(UUID inquiryId, String notes) {
        jdbc.sql("""
                 update purchase_inquiries
                 set marketing_notes = :notes, updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("id", inquiryId)
                .param("notes", notes)
                .update();
    }

    private static Instant ts(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    public record SupplierReviewRow(
            UUID id,
            String referenceCode,
            Instant submittedAt,
            String reviewState,
            String productionState,
            String websiteUrl,
            Instant approvedAt,
            UUID approvedByUserId,
            String companyName,
            String personName,
            String email,
            String phone
    ) {}

    public record BuyerLeadRow(
            UUID id,
            String referenceCode,
            Instant submittedAt,
            String leadState,
            String marketingNotes,
            String companyName,
            String personName,
            String email,
            String phone,
            String requirement
    ) {}
}
