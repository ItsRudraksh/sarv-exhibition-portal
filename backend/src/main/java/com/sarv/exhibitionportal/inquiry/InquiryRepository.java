package com.sarv.exhibitionportal.inquiry;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.CardFileDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class InquiryRepository {

    private final JdbcClient jdbc;

    public InquiryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insertDraft(UUID id, String referenceCode, String entryChannel, UUID campaignId, UUID exhibitionId) {
        jdbc.sql("""
                 insert into inquiries (
                     id, reference_code, route, entry_channel, qr_campaign_id, exhibition_id,
                     lifecycle_state, ui_step, contact_confirmed
                 ) values (
                     :id, :ref, null, :channel, :campaign, :exhibition, 'DRAFT', 'card-capture', false
                 )
                 """)
                .param("id", id)
                .param("ref", referenceCode)
                .param("channel", entryChannel)
                .param("campaign", campaignId)
                .param("exhibition", exhibitionId)
                .update();
        jdbc.sql("insert into inquiry_ui_state (inquiry_id) values (:id)")
                .param("id", id)
                .update();
    }

    public Optional<UUID> exhibitionIdForCampaign(UUID campaignId) {
        return jdbc.sql("select exhibition_id from qr_campaigns where id = :id")
                .param("id", campaignId)
                .query(UUID.class)
                .optional();
    }

    public boolean exists(UUID id) {
        Long count = jdbc.sql("select count(*) from inquiries where id = :id")
                .param("id", id)
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    public Optional<InquiryDraftDto> findDraft(UUID id) {
        Optional<InquiryRow> row = jdbc.sql("""
                select i.id, i.reference_code, i.route, i.entry_channel, i.lifecycle_state,
                       i.submitted_at, i.ui_step, i.contact_confirmed,
                       p.person_name_submitted, p.email_submitted, p.phone_submitted, p.phone_e164,
                       p.company_name_submitted, p.job_title_submitted, p.role,
                       s.website_url, s.catalogue_filename, s.catalogue_media_type, s.catalogue_byte_size,
                       s.catalogue_asset_id,
                       u.card_front_name, u.card_front_size, u.card_front_type, u.card_front_asset_id,
                       u.card_back_name, u.card_back_size, u.card_back_type, u.card_back_asset_id,
                       u.card_qr_payload_internal,
                       u.location_from_card,
                       pli.requirement_text, pli.quantity_text, pli.pack_size_text, pli.needed_by_date,
                       pli.notes, pli.product_area_search, pli.standard_code
                from inquiries i
                left join inquiry_parties p on p.inquiry_id = i.id
                left join supplier_inquiries s on s.inquiry_id = i.id
                left join inquiry_ui_state u on u.inquiry_id = i.id
                left join purchase_line_items pli on pli.purchase_inquiry_id = i.id
                where i.id = :id
                """)
                .param("id", id)
                .query((rs, n) -> new InquiryRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("reference_code"),
                        rs.getString("route"),
                        rs.getString("entry_channel"),
                        rs.getString("lifecycle_state"),
                        ts(rs.getTimestamp("submitted_at")),
                        rs.getString("ui_step"),
                        rs.getBoolean("contact_confirmed"),
                        rs.getString("person_name_submitted"),
                        rs.getString("email_submitted"),
                        rs.getString("phone_submitted"),
                        rs.getString("phone_e164"),
                        rs.getString("company_name_submitted"),
                        rs.getString("job_title_submitted"),
                        rs.getString("website_url"),
                        rs.getString("catalogue_filename"),
                        rs.getString("catalogue_media_type"),
                        longOrNull(rs.getObject("catalogue_byte_size")),
                        rs.getObject("catalogue_asset_id", UUID.class),
                        rs.getString("card_front_name"),
                        longOrNull(rs.getObject("card_front_size")),
                        rs.getString("card_front_type"),
                        rs.getObject("card_front_asset_id", UUID.class),
                        rs.getString("card_back_name"),
                        longOrNull(rs.getObject("card_back_size")),
                        rs.getString("card_back_type"),
                        rs.getObject("card_back_asset_id", UUID.class),
                        rs.getString("card_qr_payload_internal"),
                        rs.getString("location_from_card"),
                        rs.getString("requirement_text"),
                        rs.getString("quantity_text"),
                        rs.getString("pack_size_text"),
                        rs.getString("needed_by_date"),
                        rs.getString("notes"),
                        rs.getString("product_area_search"),
                        rs.getString("standard_code")
                ))
                .optional();
        if (row.isEmpty()) {
            return Optional.empty();
        }
        InquiryRow r = row.get();
        List<UUID> departments = jdbc.sql(
                        "select department_id from supplier_inquiry_departments where inquiry_id = :id")
                .param("id", id)
                .query(UUID.class)
                .list();
        List<UUID> productTypes = jdbc.sql("""
                        select distinct product_type_id
                        from supplier_inquiry_product_types
                        where inquiry_id = :id
                        """)
                .param("id", id)
                .query(UUID.class)
                .list();
        return Optional.of(toDto(r, departments, productTypes));
    }

    public void saveDraft(InquiryDraftDto draft) {
        jdbc.sql("""
                 update inquiries
                 set route = :route,
                     entry_channel = :channel,
                     lifecycle_state = :life,
                     submitted_at = :submitted,
                     ui_step = :step,
                     contact_confirmed = :confirmed,
                     updated_at = CURRENT_TIMESTAMP
                 where id = :id
                 """)
                .param("route", draft.route())
                .param("channel", draft.entryChannel() == null ? "EXHIBITION_QR" : draft.entryChannel())
                .param("life", draft.lifecycleState())
                .param("submitted", draft.submittedAt() == null ? null : Timestamp.from(draft.submittedAt()))
                .param("step", draft.currentStep() == null ? "card-capture" : draft.currentStep())
                .param("confirmed", draft.contactConfirmed())
                .param("id", draft.id())
                .update();

        replaceParty(draft);
        replaceSupplier(draft);
        replacePurchase(draft);
        replaceUiState(draft);
    }

    public void insertWorkflowEvent(UUID inquiryId, String workflow, String from, String to) {
        insertWorkflowEvent(inquiryId, workflow, from, to, "VISITOR", null);
    }

    public void insertWorkflowEvent(
            UUID inquiryId,
            String workflow,
            String from,
            String to,
            String actorKind,
            UUID actorUserId
    ) {
        jdbc.sql("""
                 insert into workflow_events (
                     id, inquiry_id, workflow, from_state, to_state, actor_kind, actor_user_id
                 ) values (
                     :id, :inquiry, :workflow, :from, :to, :actor, :user
                 )
                 """)
                .param("id", UUID.randomUUID())
                .param("inquiry", inquiryId)
                .param("workflow", workflow)
                .param("from", from)
                .param("to", to)
                .param("actor", actorKind)
                .param("user", actorUserId)
                .update();
    }

    public void markSupplierReviewSubmitted(UUID inquiryId) {
        jdbc.sql("""
                 update supplier_inquiries
                 set review_state = 'SUBMITTED', updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id and review_state = 'DRAFT'
                 """)
                .param("id", inquiryId)
                .update();
    }

    public void markPurchaseLeadSubmitted(UUID inquiryId) {
        jdbc.sql("""
                 update purchase_inquiries
                 set lead_state = 'SUBMITTED', updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id and lead_state = 'DRAFT'
                 """)
                .param("id", inquiryId)
                .update();
    }

    private void replaceParty(InquiryDraftDto draft) {
        jdbc.sql("delete from inquiry_parties where inquiry_id = :id")
                .param("id", draft.id())
                .update();
        ContactDto contact = draft.contact();
        if (contact == null || isBlank(contact.fullName()) || isBlank(contact.workEmail())) {
            return;
        }
        String role = "PURCHASE".equals(draft.route()) ? "BUYER_CONTACT" : "SUPPLIER_CONTACT";
        if (draft.route() == null) {
            role = "SUPPLIER_CONTACT";
        }
        String company = draft.supplier() == null ? null : emptyToNull(draft.supplier().companyName());
        String job = draft.supplier() == null ? null : emptyToNull(draft.supplier().jobTitle());
        String phone = joinPhone(contact);
        jdbc.sql("""
                 insert into inquiry_parties (
                     id, inquiry_id, role, company_name_submitted, person_name_submitted,
                     email_submitted, email_normalized, phone_submitted, phone_e164, job_title_submitted
                 ) values (
                     :pid, :id, :role, :company, :name, :email, :emailNorm, :phone, :e164, :job
                 )
                 """)
                .param("pid", UUID.randomUUID())
                .param("id", draft.id())
                .param("role", role)
                .param("company", company)
                .param("name", contact.fullName().trim())
                .param("email", contact.workEmail().trim())
                .param("emailNorm", contact.workEmail().trim().toLowerCase())
                .param("phone", phone)
                .param("e164", phone)
                .param("job", job)
                .update();
    }

    private void replaceSupplier(InquiryDraftDto draft) {
        if (!"SUPPLIER".equals(draft.route())) {
            return;
        }
        SupplierDto supplier = draft.supplier() == null
                ? new SupplierDto("", "", "", "", null)
                : draft.supplier();
        CardFileDto cat = supplier.catalogueFile();
        jdbc.sql("""
                 insert into supplier_inquiries (
                     inquiry_id, website_url, catalogue_filename, catalogue_media_type, catalogue_byte_size,
                     catalogue_asset_id
                 ) values (:id, :url, :fname, :mtype, :size, :asset)
                 on conflict (inquiry_id) do update set
                     website_url = excluded.website_url,
                     catalogue_filename = coalesce(excluded.catalogue_filename, supplier_inquiries.catalogue_filename),
                     catalogue_media_type = coalesce(excluded.catalogue_media_type, supplier_inquiries.catalogue_media_type),
                     catalogue_byte_size = coalesce(excluded.catalogue_byte_size, supplier_inquiries.catalogue_byte_size),
                     catalogue_asset_id = coalesce(excluded.catalogue_asset_id, supplier_inquiries.catalogue_asset_id),
                     updated_at = CURRENT_TIMESTAMP
                 """)
                .param("id", draft.id())
                .param("url", emptyToNull(supplier.websiteUrl()))
                .param("fname", cat == null ? null : emptyToNull(cat.name()))
                .param("mtype", cat == null ? null : emptyToNull(cat.type()))
                .param("size", cat == null ? null : cat.size())
                .param("asset", cat == null ? null : cat.assetId())
                .update();
        jdbc.sql("delete from supplier_inquiry_product_types where inquiry_id = :id")
                .param("id", draft.id())
                .update();
        jdbc.sql("delete from supplier_inquiry_departments where inquiry_id = :id")
                .param("id", draft.id())
                .update();
        List<UUID> departments = draft.departmentIds() == null ? List.of() : draft.departmentIds();
        for (UUID departmentId : departments) {
            jdbc.sql("insert into supplier_inquiry_departments (inquiry_id, department_id) values (:id, :d)")
                    .param("id", draft.id())
                    .param("d", departmentId)
                    .update();
        }
        List<UUID> productTypes = draft.productTypeIds() == null ? List.of() : draft.productTypeIds();
        for (UUID productTypeId : productTypes) {
            boolean mapped = false;
            for (UUID departmentId : departments) {
                Long count = jdbc.sql("""
                                         select count(*) from department_product_types
                                         where department_id = :d and product_type_id = :p and is_active = true
                                         """)
                        .param("d", departmentId)
                        .param("p", productTypeId)
                        .query(Long.class)
                        .single();
                if (count != null && count > 0) {
                    jdbc.sql("""
                             insert into supplier_inquiry_product_types (inquiry_id, department_id, product_type_id)
                             values (:id, :d, :p)
                             """)
                            .param("id", draft.id())
                            .param("d", departmentId)
                            .param("p", productTypeId)
                            .update();
                    mapped = true;
                }
            }
            if (!mapped && !departments.isEmpty()) {
                throw new InquiryValidationException(
                        "Selected product type is not valid for the chosen departments.");
            }
        }
    }

    private void replacePurchase(InquiryDraftDto draft) {
        jdbc.sql("delete from purchase_line_items where purchase_inquiry_id = :id")
                .param("id", draft.id())
                .update();
        if (!"PURCHASE".equals(draft.route())) {
            return;
        }
        jdbc.sql("""
                 insert into purchase_inquiries (inquiry_id) values (:id)
                 on conflict (inquiry_id) do update set updated_at = CURRENT_TIMESTAMP
                 """)
                .param("id", draft.id())
                .update();
        BuyerDto buyer = draft.buyer() == null
                ? new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", ""))
                : draft.buyer();
        BuyerSpecificationsDto spec = buyer.specifications() == null
                ? new BuyerSpecificationsDto("", "", "", "", "")
                : buyer.specifications();
        String requirement = emptyToNull(buyer.requirement());
        if (requirement == null) {
            requirement = null;
        }
        jdbc.sql("""
                 insert into purchase_line_items (
                     id, purchase_inquiry_id, requirement_text, quantity_text, pack_size_text,
                     needed_by_date, notes, product_area_search, standard_code, display_order
                 ) values (
                     :lid, :id, :req, :qty, :pack, :needed, :notes, :area, :std, 0
                 )
                 """)
                .param("lid", UUID.randomUUID())
                .param("id", draft.id())
                .param("req", requirement)
                .param("qty", emptyToNull(spec.quantity()))
                .param("pack", emptyToNull(spec.packSize()))
                .param("needed", emptyToNull(spec.neededByDate()))
                .param("notes", emptyToNull(spec.notes()))
                .param("area", emptyToNull(buyer.productAreaSearch()))
                .param("std", emptyToNull(spec.standard()))
                .update();
    }

    private void replaceUiState(InquiryDraftDto draft) {
        CardFileDto front = draft.cardFront();
        CardFileDto back = draft.cardBack();
        jdbc.sql("""
                 update inquiry_ui_state
                 set card_front_name = :fn, card_front_size = :fs, card_front_type = :ft,
                     card_front_asset_id = coalesce(:fa, card_front_asset_id),
                     card_back_name = :bn, card_back_size = :bs, card_back_type = :bt,
                     card_back_asset_id = coalesce(:ba, card_back_asset_id),
                     card_qr_payload_internal = :qr,
                     location_from_card = :loc,
                     updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("fn", front == null ? null : emptyToNull(front.name()))
                .param("fs", front == null ? null : front.size())
                .param("ft", front == null ? null : emptyToNull(front.type()))
                .param("fa", front == null ? null : front.assetId())
                .param("bn", back == null ? null : emptyToNull(back.name()))
                .param("bs", back == null ? null : back.size())
                .param("bt", back == null ? null : emptyToNull(back.type()))
                .param("ba", back == null ? null : back.assetId())
                .param("qr", emptyToNull(draft.cardQrPayloadInternal()))
                .param("loc", draft.supplier() == null ? null : emptyToNull(draft.supplier().locationFromCard()))
                .param("id", draft.id())
                .update();
    }

    private static InquiryDraftDto toDto(InquiryRow r, List<UUID> departments, List<UUID> productTypes) {
        String[] phone = splitPhone(r.phoneSubmitted(), r.phoneE164());
        ContactDto contact = new ContactDto(
                nvl(r.personName()),
                nvl(r.email()),
                phone[0],
                phone[1]
        );
        CardFileDto catalogue = r.catalogueFilename() == null
                ? null
                : new CardFileDto(r.catalogueFilename(), r.catalogueSize(), r.catalogueType(), r.catalogueAssetId());
        SupplierDto supplier = new SupplierDto(
                nvl(r.companyName()),
                nvl(r.websiteUrl()),
                nvl(r.jobTitle()),
                nvl(r.locationFromCard()),
                catalogue
        );
        BuyerSpecificationsDto specs = new BuyerSpecificationsDto(
                nvl(r.quantity()),
                nvl(r.packSize()),
                nvl(r.standardCode()),
                nvl(r.neededBy()),
                nvl(r.notes())
        );
        BuyerDto buyer = new BuyerDto(nvl(r.requirement()), nvl(r.productArea()), specs);
        return new InquiryDraftDto(
                r.id(),
                r.lifecycle(),
                nvl(r.uiStep(), "card-capture"),
                r.route(),
                r.entryChannel(),
                card(r.frontName(), r.frontSize(), r.frontType(), r.frontAssetId()),
                card(r.backName(), r.backSize(), r.backType(), r.backAssetId()),
                r.qrPayload(),
                contact,
                supplier,
                new ArrayList<>(departments),
                new ArrayList<>(productTypes),
                buyer,
                r.contactConfirmed(),
                r.submittedAt(),
                r.referenceCode()
        );
    }

    private static CardFileDto card(String name, Long size, String type, UUID assetId) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return new CardFileDto(name, size, type, assetId);
    }

    private static String[] splitPhone(String submitted, String e164) {
        String raw = submitted != null ? submitted : e164;
        if (raw == null || raw.isBlank()) {
            return new String[] {"+91", ""};
        }
        String trimmed = raw.trim();
        int space = trimmed.indexOf(' ');
        if (trimmed.startsWith("+") && space > 0) {
            return new String[] {trimmed.substring(0, space), trimmed.substring(space + 1)};
        }
        if (trimmed.startsWith("+")) {
            return new String[] {trimmed, ""};
        }
        return new String[] {"+91", trimmed};
    }

    private static String joinPhone(ContactDto contact) {
        String code = contact.countryCode() == null ? "" : contact.countryCode().trim();
        String number = contact.mobileNumber() == null ? "" : contact.mobileNumber().trim();
        return (code + " " + number).trim();
    }

    private static Instant ts(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Long longOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String emptyToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    private static String nvl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record InquiryRow(
            UUID id,
            String referenceCode,
            String route,
            String entryChannel,
            String lifecycle,
            Instant submittedAt,
            String uiStep,
            boolean contactConfirmed,
            String personName,
            String email,
            String phoneSubmitted,
            String phoneE164,
            String companyName,
            String jobTitle,
            String websiteUrl,
            String catalogueFilename,
            String catalogueType,
            Long catalogueSize,
            UUID catalogueAssetId,
            String frontName,
            Long frontSize,
            String frontType,
            UUID frontAssetId,
            String backName,
            Long backSize,
            String backType,
            UUID backAssetId,
            String qrPayload,
            String locationFromCard,
            String requirement,
            String quantity,
            String packSize,
            String neededBy,
            String notes,
            String productArea,
            String standardCode
    ) {}
}
