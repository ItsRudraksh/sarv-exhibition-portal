package com.sarv.exhibitionportal.inquiry;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.CardFileDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import com.sarv.exhibitionportal.config.JdbcUuids;
import com.sarv.exhibitionportal.finishedgoods.FinishedGoodsRepository;
import com.sarv.exhibitionportal.fileasset.FileAssetRepository;
import com.sarv.exhibitionportal.trading.TradingCatalogueRepository;
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
    private final FinishedGoodsRepository finishedGoods;
    private final TradingCatalogueRepository trading;
    private final FileAssetRepository files;

    public InquiryRepository(
            JdbcClient jdbc,
            FinishedGoodsRepository finishedGoods,
            TradingCatalogueRepository trading,
            FileAssetRepository files
    ) {
        this.jdbc = jdbc;
        this.finishedGoods = finishedGoods;
        this.trading = trading;
        this.files = files;
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
                .param("id", JdbcUuids.mysql(id))
                .param("ref", JdbcUuids.mysql(referenceCode))
                .param("channel", JdbcUuids.mysql(entryChannel))
                .param("campaign", JdbcUuids.mysql(campaignId))
                .param("exhibition", JdbcUuids.mysql(exhibitionId))
                .update();
        jdbc.sql("insert into inquiry_ui_state (inquiry_id) values (:id)")
                .param("id", JdbcUuids.mysql(id))
                .update();
    }

    public Optional<UUID> exhibitionIdForCampaign(UUID campaignId) {
        return jdbc.sql("select exhibition_id from qr_campaigns where id = :id")
                .param("id", JdbcUuids.mysql(campaignId))
                .query(String.class)
                .optional()
                .flatMap(JdbcUuids::optional);
    }

    public boolean exists(UUID id) {
        Long count = jdbc.sql("select count(*) from inquiries where id = :id")
                .param("id", JdbcUuids.mysql(id))
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
                       s.catalogue_asset_id, s.other_category, s.other_product_type,
                       s.other_category_detail, s.other_product_type_detail, s.capability_notes,
                       u.card_front_name, u.card_front_size, u.card_front_type, u.card_front_asset_id,
                       u.card_back_name, u.card_back_size, u.card_back_type, u.card_back_asset_id,
                       u.card_qr_payload_internal,
                       u.location_from_card,
                       pli.requirement_text, pli.quantity_text, pli.pack_size_text, pli.needed_by_date,
                       pli.notes, pli.product_area_search, pli.standard_code,
                       pi.other_product, pi.other_product_detail
                from inquiries i
                left join inquiry_parties p on p.inquiry_id = i.id
                left join supplier_inquiries s on s.inquiry_id = i.id
                left join inquiry_ui_state u on u.inquiry_id = i.id
                left join purchase_inquiries pi on pi.inquiry_id = i.id
                left join purchase_line_items pli on pli.purchase_inquiry_id = i.id
                where i.id = :id
                """)
                .param("id", JdbcUuids.mysql(id))
                .query((rs, n) -> new InquiryRow(
                        JdbcUuids.get(rs, "id"),
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
                        JdbcUuids.get(rs, "catalogue_asset_id"),
                        rs.getBoolean("other_category"),
                        rs.getBoolean("other_product_type"),
                        rs.getString("other_category_detail"),
                        rs.getString("other_product_type_detail"),
                        rs.getString("capability_notes"),
                        rs.getString("card_front_name"),
                        longOrNull(rs.getObject("card_front_size")),
                        rs.getString("card_front_type"),
                        JdbcUuids.get(rs, "card_front_asset_id"),
                        rs.getString("card_back_name"),
                        longOrNull(rs.getObject("card_back_size")),
                        rs.getString("card_back_type"),
                        JdbcUuids.get(rs, "card_back_asset_id"),
                        rs.getString("card_qr_payload_internal"),
                        rs.getString("location_from_card"),
                        rs.getString("requirement_text"),
                        rs.getString("quantity_text"),
                        rs.getString("pack_size_text"),
                        rs.getString("needed_by_date"),
                        rs.getString("notes"),
                        rs.getString("product_area_search"),
                        rs.getString("standard_code"),
                        rs.getBoolean("other_product"),
                        rs.getString("other_product_detail")
                ))
                .optional();
        if (row.isEmpty()) {
            return Optional.empty();
        }
        InquiryRow r = row.get();
        List<UUID> departments = jdbc.sql(
                        "select department_id from supplier_inquiry_departments where inquiry_id = :id")
                .param("id", JdbcUuids.mysql(id))
                .query(String.class)
                .list()
                .stream()
                .map(UUID::fromString)
                .toList();
        List<UUID> productTypes = jdbc.sql("""
                        select distinct product_type_id
                        from supplier_inquiry_product_types
                        where inquiry_id = :id
                        """)
                .param("id", JdbcUuids.mysql(id))
                .query(String.class)
                .list()
                .stream()
                .map(UUID::fromString)
                .toList();
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
                .param("route", JdbcUuids.mysql(draft.route()))
                .param("channel", JdbcUuids.mysql(draft.entryChannel() == null ? "EXHIBITION_QR" : draft.entryChannel()))
                .param("life", JdbcUuids.mysql(draft.lifecycleState()))
                .param("submitted", JdbcUuids.mysql(draft.submittedAt() == null ? null : Timestamp.from(draft.submittedAt())))
                .param("step", JdbcUuids.mysql(draft.currentStep() == null ? "card-capture" : draft.currentStep()))
                .param("confirmed", JdbcUuids.mysql(draft.contactConfirmed()))
                .param("id", JdbcUuids.mysql(draft.id()))
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
                .param("id", JdbcUuids.mysql(UUID.randomUUID()))
                .param("inquiry", JdbcUuids.mysql(inquiryId))
                .param("workflow", JdbcUuids.mysql(workflow))
                .param("from", JdbcUuids.mysql(from))
                .param("to", JdbcUuids.mysql(to))
                .param("actor", JdbcUuids.mysql(actorKind))
                .param("user", JdbcUuids.mysql(actorUserId))
                .update();
    }

    public void markSupplierReviewSubmitted(UUID inquiryId) {
        jdbc.sql("""
                 update supplier_inquiries
                 set review_state = 'SUBMITTED', updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id and review_state = 'DRAFT'
                 """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
    }

    public void markPurchaseLeadSubmitted(UUID inquiryId) {
        jdbc.sql("""
                 update purchase_inquiries
                 set lead_state = 'SUBMITTED', updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id and lead_state = 'DRAFT'
                 """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
    }

    private void replaceParty(InquiryDraftDto draft) {
        jdbc.sql("delete from inquiry_parties where inquiry_id = :id")
                .param("id", JdbcUuids.mysql(draft.id()))
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
                .param("pid", JdbcUuids.mysql(UUID.randomUUID()))
                .param("id", JdbcUuids.mysql(draft.id()))
                .param("role", JdbcUuids.mysql(role))
                .param("company", JdbcUuids.mysql(company))
                .param("name", JdbcUuids.mysql(contact.fullName().trim()))
                .param("email", JdbcUuids.mysql(contact.workEmail().trim()))
                .param("emailNorm", JdbcUuids.mysql(contact.workEmail().trim().toLowerCase()))
                .param("phone", JdbcUuids.mysql(phone))
                .param("e164", JdbcUuids.mysql(phone))
                .param("job", JdbcUuids.mysql(job))
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
                     catalogue_asset_id, other_category, other_product_type,
                     other_category_detail, other_product_type_detail, capability_notes
                 ) values (
                     :id, :url, :fname, :mtype, :size, :asset, :otherCat, :otherType,
                     :otherCatDetail, :otherTypeDetail, :notes
                 )
                 on duplicate key update
                     website_url = VALUES(website_url),
                     catalogue_filename = COALESCE(VALUES(catalogue_filename), catalogue_filename),
                     catalogue_media_type = COALESCE(VALUES(catalogue_media_type), catalogue_media_type),
                     catalogue_byte_size = COALESCE(VALUES(catalogue_byte_size), catalogue_byte_size),
                     catalogue_asset_id = COALESCE(VALUES(catalogue_asset_id), catalogue_asset_id),
                     other_category = VALUES(other_category),
                     other_product_type = VALUES(other_product_type),
                     other_category_detail = VALUES(other_category_detail),
                     other_product_type_detail = VALUES(other_product_type_detail),
                     capability_notes = VALUES(capability_notes)
                 """)
                .param("id", JdbcUuids.mysql(draft.id()))
                .param("url", JdbcUuids.mysql(emptyToNull(supplier.websiteUrl())))
                .param("fname", JdbcUuids.mysql(cat == null ? null : emptyToNull(cat.name())))
                .param("mtype", JdbcUuids.mysql(cat == null ? null : emptyToNull(cat.type())))
                .param("size", JdbcUuids.mysql(cat == null ? null : cat.size()))
                .param("asset", JdbcUuids.mysql(cat == null ? null : cat.assetId()))
                .param("otherCat", JdbcUuids.mysql(supplier.otherCategory()))
                .param("otherType", JdbcUuids.mysql(supplier.otherProductType()))
                .param("otherCatDetail", JdbcUuids.mysql(emptyToNull(supplier.otherCategoryDetail())))
                .param("otherTypeDetail", JdbcUuids.mysql(emptyToNull(supplier.otherProductTypeDetail())))
                .param("notes", JdbcUuids.mysql(emptyToNull(supplier.capabilityNotes())))
                .update();
        jdbc.sql("delete from supplier_inquiry_product_types where inquiry_id = :id")
                .param("id", JdbcUuids.mysql(draft.id()))
                .update();
        jdbc.sql("delete from supplier_inquiry_departments where inquiry_id = :id")
                .param("id", JdbcUuids.mysql(draft.id()))
                .update();
        List<UUID> departments = draft.departmentIds() == null ? List.of() : draft.departmentIds();
        for (UUID departmentId : departments) {
            jdbc.sql("insert into supplier_inquiry_departments (inquiry_id, department_id) values (:id, :d)")
                    .param("id", JdbcUuids.mysql(draft.id()))
                    .param("d", JdbcUuids.mysql(departmentId))
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
                        .param("d", JdbcUuids.mysql(departmentId))
                        .param("p", JdbcUuids.mysql(productTypeId))
                        .query(Long.class)
                        .single();
                if (count != null && count > 0) {
                    jdbc.sql("""
                             insert into supplier_inquiry_product_types (inquiry_id, department_id, product_type_id)
                             values (:id, :d, :p)
                             """)
                            .param("id", JdbcUuids.mysql(draft.id()))
                            .param("d", JdbcUuids.mysql(departmentId))
                            .param("p", JdbcUuids.mysql(productTypeId))
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
                .param("id", JdbcUuids.mysql(draft.id()))
                .update();
        if (!"PURCHASE".equals(draft.route())) {
            finishedGoods.replaceInquirySelections(draft.id(), List.of());
            trading.replaceInquirySelections(draft.id(), List.of());
            return;
        }
        BuyerDto buyer = draft.buyer() == null
                ? new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", ""))
                : draft.buyer();
        jdbc.sql("""
                 insert into purchase_inquiries (inquiry_id, other_product, other_product_detail)
                 values (:id, :other, :detail)
                 on duplicate key update
                     other_product = VALUES(other_product),
                     other_product_detail = VALUES(other_product_detail)
                 """)
                .param("id", JdbcUuids.mysql(draft.id()))
                .param("other", JdbcUuids.mysql(buyer.otherProduct()))
                .param("detail", JdbcUuids.mysql(emptyToNull(buyer.otherProductDetail())))
                .update();
        BuyerSpecificationsDto spec = buyer.specifications() == null
                ? new BuyerSpecificationsDto("", "", "", "", "")
                : buyer.specifications();
        String requirement = emptyToNull(buyer.requirement());
        if (requirement == null && buyer.otherProduct()) {
            requirement = emptyToNull(buyer.otherProductDetail());
        }
        jdbc.sql("""
                 insert into purchase_line_items (
                     id, purchase_inquiry_id, requirement_text, quantity_text, pack_size_text,
                     needed_by_date, notes, product_area_search, standard_code, display_order
                 ) values (
                     :lid, :id, :req, :qty, :pack, :needed, :notes, :area, :std, 0
                 )
                 """)
                .param("lid", JdbcUuids.mysql(UUID.randomUUID()))
                .param("id", JdbcUuids.mysql(draft.id()))
                .param("req", JdbcUuids.mysql(requirement))
                .param("qty", JdbcUuids.mysql(emptyToNull(spec.quantity())))
                .param("pack", JdbcUuids.mysql(emptyToNull(spec.packSize())))
                .param("needed", JdbcUuids.mysql(emptyToNull(spec.neededByDate())))
                .param("notes", JdbcUuids.mysql(emptyToNull(spec.notes())))
                .param("area", JdbcUuids.mysql(emptyToNull(buyer.productAreaSearch())))
                .param("std", JdbcUuids.mysql(emptyToNull(spec.standard())))
                .update();
        finishedGoods.replaceInquirySelections(draft.id(), buyer.finishedGoods());
        if (buyer.tradingProducts() != null) {
            for (var row : buyer.tradingProducts()) {
                if (row != null && row.tradingProductId() != null && !trading.isSelectable(row.tradingProductId())) {
                    throw new InquiryValidationException(
                            "A selected trading product is not listed for buyers.");
                }
            }
        }
        trading.replaceInquirySelections(draft.id(), buyer.tradingProducts());
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
                     location_from_card = :loc,
                     updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("fn", JdbcUuids.mysql(front == null ? null : emptyToNull(front.name())))
                .param("fs", JdbcUuids.mysql(front == null ? null : front.size()))
                .param("ft", JdbcUuids.mysql(front == null ? null : emptyToNull(front.type())))
                .param("fa", JdbcUuids.mysql(front == null ? null : front.assetId()))
                .param("bn", JdbcUuids.mysql(back == null ? null : emptyToNull(back.name())))
                .param("bs", JdbcUuids.mysql(back == null ? null : back.size()))
                .param("bt", JdbcUuids.mysql(back == null ? null : emptyToNull(back.type())))
                .param("ba", JdbcUuids.mysql(back == null ? null : back.assetId()))
                .param("loc", JdbcUuids.mysql(draft.supplier() == null ? null : emptyToNull(draft.supplier().locationFromCard())))
                .param("id", JdbcUuids.mysql(draft.id()))
                .update();
    }

    /** Server-only: card QR payloads are never accepted from visitor PATCH bodies. */
    public void updateCardQrPayload(UUID inquiryId, String payload) {
        jdbc.sql("""
                 update inquiry_ui_state
                 set card_qr_payload_internal = :qr, updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("qr", JdbcUuids.mysql(emptyToNull(payload)))
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
    }

    private InquiryDraftDto toDto(InquiryRow r, List<UUID> departments, List<UUID> productTypes) {
        String[] phone = splitPhone(r.phoneSubmitted(), r.phoneE164());
        ContactDto contact = new ContactDto(
                nvl(r.personName()),
                nvl(r.email()),
                phone[0],
                phone[1]
        );
        List<CardFileDto> attachments = files.supportingCards(r.id());
        CardFileDto catalogue = r.catalogueFilename() == null
                ? (attachments.isEmpty() ? null : attachments.get(0))
                : new CardFileDto(r.catalogueFilename(), r.catalogueSize(), r.catalogueType(), r.catalogueAssetId());
        SupplierDto supplier = new SupplierDto(
                nvl(r.companyName()),
                nvl(r.websiteUrl()),
                nvl(r.jobTitle()),
                nvl(r.locationFromCard()),
                catalogue,
                r.otherCategory(),
                r.otherProductType(),
                nvl(r.otherCategoryDetail()),
                nvl(r.otherProductTypeDetail()),
                nvl(r.capabilityNotes()),
                attachments
        );
        BuyerSpecificationsDto specs = new BuyerSpecificationsDto(
                nvl(r.quantity()),
                nvl(r.packSize()),
                nvl(r.standardCode()),
                nvl(r.neededBy()),
                nvl(r.notes())
        );
        BuyerDto buyer = new BuyerDto(
                nvl(r.requirement()),
                nvl(r.productArea()),
                specs,
                finishedGoods.selectionsForInquiry(r.id()),
                trading.selectionsForInquiry(r.id()),
                attachments,
                r.otherProduct(),
                nvl(r.otherProductDetail()));
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
            boolean otherCategory,
            boolean otherProductType,
            String otherCategoryDetail,
            String otherProductTypeDetail,
            String capabilityNotes,
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
            String standardCode,
            boolean otherProduct,
            String otherProductDetail
    ) {}
}
