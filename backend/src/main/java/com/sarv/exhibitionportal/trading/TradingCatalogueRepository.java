package com.sarv.exhibitionportal.trading;

import com.sarv.exhibitionportal.api.dto.BuyerProductDto;
import com.sarv.exhibitionportal.api.dto.BuyerTradingProductDto;
import com.sarv.exhibitionportal.api.dto.PortalSupplierCandidateDto;
import com.sarv.exhibitionportal.api.dto.TradingProductDto;
import com.sarv.exhibitionportal.api.dto.TradingSupplierDto;
import com.sarv.exhibitionportal.config.JdbcUuids;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TradingCatalogueRepository {

    private final JdbcClient jdbc;

    public TradingCatalogueRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<TradingSupplierDto> listSuppliers() {
        List<SupplierRow> rows = jdbc.sql("""
                select id, source_kind, portal_inquiry_id, company_name, contact_name, email, phone,
                       website_url, notes, status
                from trading_suppliers
                order by company_name
                """)
                .query((rs, n) -> new SupplierRow(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("source_kind"),
                        JdbcUuids.get(rs, "portal_inquiry_id"),
                        rs.getString("company_name"),
                        rs.getString("contact_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("website_url"),
                        rs.getString("notes"),
                        rs.getString("status")
                ))
                .list();
        Map<UUID, List<TradingProductDto>> products = productsBySupplier();
        List<TradingSupplierDto> out = new ArrayList<>();
        for (SupplierRow row : rows) {
            out.add(toDto(row, products.getOrDefault(row.id(), List.of())));
        }
        return out;
    }

    public Optional<TradingSupplierDto> findSupplier(UUID id) {
        return jdbc.sql("""
                select id, source_kind, portal_inquiry_id, company_name, contact_name, email, phone,
                       website_url, notes, status
                from trading_suppliers
                where id = :id
                """)
                .param("id", JdbcUuids.mysql(id))
                .query((rs, n) -> new SupplierRow(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("source_kind"),
                        JdbcUuids.get(rs, "portal_inquiry_id"),
                        rs.getString("company_name"),
                        rs.getString("contact_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("website_url"),
                        rs.getString("notes"),
                        rs.getString("status")
                ))
                .optional()
                .map(row -> toDto(row, listProducts(row.id())));
    }

    public Optional<UUID> supplierIdForPortalInquiry(UUID inquiryId) {
        return jdbc.sql("select id from trading_suppliers where portal_inquiry_id = :id")
                .param("id", JdbcUuids.mysql(inquiryId))
                .query((rs, n) -> JdbcUuids.get(rs, "id"))
                .optional();
    }

    public void insertSupplier(
            UUID id,
            String sourceKind,
            UUID portalInquiryId,
            String companyName,
            String contactName,
            String email,
            String phone,
            String websiteUrl,
            String notes,
            UUID createdBy
    ) {
        jdbc.sql("""
                insert into trading_suppliers (
                    id, source_kind, portal_inquiry_id, company_name, contact_name, email, phone,
                    website_url, notes, status, created_by_user_id
                ) values (
                    :id, :kind, :portal, :company, :contact, :email, :phone, :web, :notes, 'ACTIVE', :actor
                )
                """)
                .param("id", JdbcUuids.mysql(id))
                .param("kind", JdbcUuids.mysql(sourceKind))
                .param("portal", JdbcUuids.mysql(portalInquiryId))
                .param("company", JdbcUuids.mysql(companyName))
                .param("contact", JdbcUuids.mysql(contactName))
                .param("email", JdbcUuids.mysql(email))
                .param("phone", JdbcUuids.mysql(phone))
                .param("web", JdbcUuids.mysql(websiteUrl))
                .param("notes", JdbcUuids.mysql(notes))
                .param("actor", JdbcUuids.mysql(createdBy))
                .update();
    }

    public void updateSupplier(
            UUID id,
            String companyName,
            String contactName,
            String email,
            String phone,
            String websiteUrl,
            String notes,
            String status
    ) {
        jdbc.sql("""
                update trading_suppliers
                set company_name = :company,
                    contact_name = :contact,
                    email = :email,
                    phone = :phone,
                    website_url = :web,
                    notes = :notes,
                    status = :status,
                    updated_at = current_timestamp
                where id = :id
                """)
                .param("id", JdbcUuids.mysql(id))
                .param("company", JdbcUuids.mysql(companyName))
                .param("contact", JdbcUuids.mysql(contactName))
                .param("email", JdbcUuids.mysql(email))
                .param("phone", JdbcUuids.mysql(phone))
                .param("web", JdbcUuids.mysql(websiteUrl))
                .param("notes", JdbcUuids.mysql(notes))
                .param("status", JdbcUuids.mysql(status))
                .update();
    }

    public List<TradingProductDto> listProducts(UUID supplierId) {
        return jdbc.sql("""
                select id, name, listed_for_buyers, is_active
                from trading_products
                where supplier_id = :id
                order by display_order, name
                """)
                .param("id", JdbcUuids.mysql(supplierId))
                .query((rs, n) -> new TradingProductDto(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("name"),
                        rs.getBoolean("listed_for_buyers"),
                        rs.getBoolean("is_active")
                ))
                .list();
    }

    public Optional<TradingProductDto> findProduct(UUID supplierId, UUID productId) {
        return jdbc.sql("""
                select id, name, listed_for_buyers, is_active
                from trading_products
                where id = :pid and supplier_id = :sid
                """)
                .param("pid", JdbcUuids.mysql(productId))
                .param("sid", JdbcUuids.mysql(supplierId))
                .query((rs, n) -> new TradingProductDto(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("name"),
                        rs.getBoolean("listed_for_buyers"),
                        rs.getBoolean("is_active")
                ))
                .optional();
    }

    public boolean productNameTaken(UUID supplierId, String name, UUID excludingId) {
        Long count = jdbc.sql("""
                select count(*) from trading_products
                where supplier_id = :sid and lower(name) = lower(:name)
                  and (:exclude is null or id <> :exclude)
                """)
                .param("sid", JdbcUuids.mysql(supplierId))
                .param("name", JdbcUuids.mysql(name))
                .param("exclude", JdbcUuids.mysql(excludingId))
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    public void insertProduct(UUID id, UUID supplierId, String name, boolean listed, int displayOrder) {
        jdbc.sql("""
                insert into trading_products (id, supplier_id, name, listed_for_buyers, is_active, display_order)
                values (:id, :sid, :name, :listed, true, :ord)
                """)
                .param("id", JdbcUuids.mysql(id))
                .param("sid", JdbcUuids.mysql(supplierId))
                .param("name", JdbcUuids.mysql(name))
                .param("listed", listed)
                .param("ord", displayOrder)
                .update();
    }

    public void updateProduct(UUID id, String name, boolean listed, boolean active) {
        jdbc.sql("""
                update trading_products
                set name = :name, listed_for_buyers = :listed, is_active = :active, updated_at = current_timestamp
                where id = :id
                """)
                .param("id", JdbcUuids.mysql(id))
                .param("name", JdbcUuids.mysql(name))
                .param("listed", listed)
                .param("active", active)
                .update();
    }

    public int nextDisplayOrder(UUID supplierId) {
        Integer max = jdbc.sql("select coalesce(max(display_order), 0) from trading_products where supplier_id = :id")
                .param("id", JdbcUuids.mysql(supplierId))
                .query(Integer.class)
                .single();
        return (max == null ? 0 : max) + 10;
    }

    public List<PortalSupplierCandidateDto> portalCandidates() {
        return jdbc.sql("""
                select i.id, i.reference_code, s.review_state,
                       p.company_name_submitted, p.person_name_submitted
                from inquiries i
                join supplier_inquiries s on s.inquiry_id = i.id
                left join inquiry_parties p on p.inquiry_id = i.id and p.role = 'SUPPLIER_CONTACT'
                where i.lifecycle_state = 'SUBMITTED' and i.route = 'SUPPLIER'
                  and not exists (
                      select 1 from trading_suppliers ts where ts.portal_inquiry_id = i.id
                  )
                order by i.submitted_at desc
                """)
                .query((rs, n) -> new PortalSupplierCandidateDto(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("reference_code"),
                        rs.getString("company_name_submitted"),
                        rs.getString("person_name_submitted"),
                        rs.getString("review_state")
                ))
                .list();
    }

    public Optional<PortalParty> findSubmittedSupplierParty(UUID inquiryId) {
        return jdbc.sql("""
                select i.id, s.website_url,
                       p.company_name_submitted, p.person_name_submitted, p.email_submitted, p.phone_submitted
                from inquiries i
                join supplier_inquiries s on s.inquiry_id = i.id
                left join inquiry_parties p on p.inquiry_id = i.id and p.role = 'SUPPLIER_CONTACT'
                where i.id = :id and i.lifecycle_state = 'SUBMITTED' and i.route = 'SUPPLIER'
                """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .query((rs, n) -> new PortalParty(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("website_url"),
                        rs.getString("company_name_submitted"),
                        rs.getString("person_name_submitted"),
                        rs.getString("email_submitted"),
                        rs.getString("phone_submitted")
                ))
                .optional();
    }

    public List<String> productTypeNames(UUID inquiryId) {
        return jdbc.sql("""
                select distinct pt.name
                from supplier_inquiry_product_types spt
                join product_types pt on pt.id = spt.product_type_id
                where spt.inquiry_id = :id
                order by pt.name
                """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .query((rs, n) -> rs.getString("name"))
                .list();
    }

    public List<BuyerProductDto> searchListed(String query, int limit) {
        int lim = Math.min(Math.max(limit, 1), 200);
        String q = query == null ? "" : query.trim().toLowerCase();
        String like = q.isEmpty() ? null : "%" + q + "%";
        return jdbc.sql("""
                select tp.id, tp.name,
                       case ts.source_kind when 'PORTAL' then 'PORTAL_SUPPLIER' else 'OFFLINE_SUPPLIER' end as source_kind
                from trading_products tp
                join trading_suppliers ts on ts.id = tp.supplier_id
                where tp.is_active = true
                  and tp.listed_for_buyers = true
                  and ts.status = 'ACTIVE'
                  and (:like is null or lower(tp.name) like :like)
                order by tp.name
                limit :lim
                """)
                .param("like", like)
                .param("lim", lim)
                .query((rs, n) -> new BuyerProductDto(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("name"),
                        rs.getString("source_kind")
                ))
                .list();
    }

    public int countListedActive() {
        Integer count = jdbc.sql("""
                select count(*)
                from trading_products tp
                join trading_suppliers ts on ts.id = tp.supplier_id
                where tp.is_active = true and tp.listed_for_buyers = true and ts.status = 'ACTIVE'
                """)
                .query(Integer.class)
                .optional()
                .orElse(0);
        return count;
    }

    public boolean isSelectable(UUID productId) {
        Long count = jdbc.sql("""
                select count(*)
                from trading_products tp
                join trading_suppliers ts on ts.id = tp.supplier_id
                where tp.id = :id
                  and tp.is_active = true
                  and tp.listed_for_buyers = true
                  and ts.status = 'ACTIVE'
                """)
                .param("id", JdbcUuids.mysql(productId))
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    public void replaceInquirySelections(UUID inquiryId, List<BuyerTradingProductDto> selections) {
        jdbc.sql("delete from purchase_inquiry_trading_products where inquiry_id = :id")
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
        if (selections == null || selections.isEmpty()) {
            return;
        }
        Set<UUID> seen = new HashSet<>();
        for (BuyerTradingProductDto row : selections) {
            if (row == null || row.tradingProductId() == null || !seen.add(row.tradingProductId())) {
                continue;
            }
            String qty = row.quantity() == null ? "" : row.quantity().trim();
            jdbc.sql("""
                    insert into purchase_inquiry_trading_products (inquiry_id, trading_product_id, quantity_text)
                    values (:inq, :pid, :qty)
                    """)
                    .param("inq", JdbcUuids.mysql(inquiryId))
                    .param("pid", JdbcUuids.mysql(row.tradingProductId()))
                    .param("qty", qty)
                    .update();
        }
    }

    public List<BuyerTradingProductDto> selectionsForInquiry(UUID inquiryId) {
        return jdbc.sql("""
                select trading_product_id, quantity_text
                from purchase_inquiry_trading_products
                where inquiry_id = :id
                """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .query((rs, n) -> new BuyerTradingProductDto(
                        JdbcUuids.get(rs, "trading_product_id"),
                        rs.getString("quantity_text") == null ? "" : rs.getString("quantity_text")
                ))
                .list();
    }

    private Map<UUID, List<TradingProductDto>> productsBySupplier() {
        Map<UUID, List<TradingProductDto>> map = new LinkedHashMap<>();
        jdbc.sql("""
                select id, supplier_id, name, listed_for_buyers, is_active
                from trading_products
                order by display_order, name
                """)
                .query((rs, n) -> {
                    UUID supplierId = JdbcUuids.get(rs, "supplier_id");
                    map.computeIfAbsent(supplierId, k -> new ArrayList<>())
                            .add(new TradingProductDto(
                                    JdbcUuids.get(rs, "id"),
                                    rs.getString("name"),
                                    rs.getBoolean("listed_for_buyers"),
                                    rs.getBoolean("is_active")
                            ));
                    return 0;
                })
                .list();
        return map;
    }

    private TradingSupplierDto toDto(SupplierRow row, List<TradingProductDto> products) {
        List<String> suggested = List.of();
        if ("PORTAL".equals(row.sourceKind()) && row.portalInquiryId() != null) {
            Set<String> existing = new HashSet<>();
            for (TradingProductDto product : products) {
                existing.add(product.name().trim().toLowerCase());
            }
            List<String> names = new ArrayList<>();
            for (String name : productTypeNames(row.portalInquiryId())) {
                if (name != null && !existing.contains(name.trim().toLowerCase())) {
                    names.add(name);
                }
            }
            suggested = names;
        }
        return new TradingSupplierDto(
                row.id(),
                row.sourceKind(),
                row.portalInquiryId(),
                row.companyName(),
                row.contactName(),
                row.email(),
                row.phone(),
                row.websiteUrl(),
                row.notes(),
                row.status(),
                products,
                suggested
        );
    }

    public record PortalParty(
            UUID inquiryId,
            String websiteUrl,
            String companyName,
            String contactName,
            String email,
            String phone
    ) {}

    private record SupplierRow(
            UUID id,
            String sourceKind,
            UUID portalInquiryId,
            String companyName,
            String contactName,
            String email,
            String phone,
            String websiteUrl,
            String notes,
            String status
    ) {}
}
