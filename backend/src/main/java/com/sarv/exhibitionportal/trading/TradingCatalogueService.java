package com.sarv.exhibitionportal.trading;

import com.sarv.exhibitionportal.api.dto.BuyerProductDto;
import com.sarv.exhibitionportal.api.dto.BuyerTradingProductDto;
import com.sarv.exhibitionportal.api.dto.CreateOfflineSupplierRequest;
import com.sarv.exhibitionportal.api.dto.LinkPortalSupplierRequest;
import com.sarv.exhibitionportal.api.dto.PortalSupplierCandidateDto;
import com.sarv.exhibitionportal.api.dto.TradingProductDto;
import com.sarv.exhibitionportal.api.dto.TradingSupplierDto;
import com.sarv.exhibitionportal.api.dto.UpdateTradingSupplierRequest;
import com.sarv.exhibitionportal.api.dto.UpsertTradingProductRequest;
import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.finishedgoods.FinishedGoodsService;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import com.sarv.exhibitionportal.staff.StaffUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TradingCatalogueService {

    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final TradingCatalogueRepository trading;
    private final FinishedGoodsService finishedGoods;
    private final AuditService audits;

    public TradingCatalogueService(
            TradingCatalogueRepository trading,
            FinishedGoodsService finishedGoods,
            AuditService audits
    ) {
        this.trading = trading;
        this.finishedGoods = finishedGoods;
        this.audits = audits;
    }

    @Transactional(readOnly = true)
    public List<BuyerProductDto> listBuyerProducts(String query) {
        List<BuyerProductDto> rows = new ArrayList<>();
        finishedGoods.list(query).forEach(fg ->
                rows.add(new BuyerProductDto(fg.id(), fg.name(), "PHARMA_ERP")));
        rows.addAll(trading.searchListed(query, 200));
        rows.sort(Comparator.comparing(row -> row.name() == null ? "" : row.name().toLowerCase(Locale.ROOT)));
        if (rows.size() > 200) {
            return rows.subList(0, 200);
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public int listedTradingCount() {
        return trading.countListedActive();
    }

    @Transactional(readOnly = true)
    public List<TradingSupplierDto> listSuppliers() {
        return trading.listSuppliers();
    }

    @Transactional(readOnly = true)
    public TradingSupplierDto getSupplier(UUID id) {
        return trading.findSupplier(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trading supplier not found"));
    }

    @Transactional(readOnly = true)
    public List<PortalSupplierCandidateDto> portalCandidates() {
        return trading.portalCandidates();
    }

    @Transactional
    public TradingSupplierDto createOffline(CreateOfflineSupplierRequest request, StaffUser actor) {
        if (request == null) {
            throw new InquiryValidationException("Enter a company name.");
        }
        String company = requireName(request.companyName(), "Enter a company name.");
        UUID id = UUID.randomUUID();
        trading.insertSupplier(
                id,
                "OFFLINE",
                null,
                company,
                emptyToNull(request.contactName()),
                emptyToNull(request.email()),
                emptyToNull(request.phone()),
                emptyToNull(request.websiteUrl()),
                emptyToNull(request.notes()),
                actor.id()
        );
        audits.recordUser(null, "TRADING_SUPPLIER", id, "TRADING_SUPPLIER_CREATED", actor.id(),
                Map.of("sourceKind", "OFFLINE"));
        return getSupplier(id);
    }

    @Transactional
    public TradingSupplierDto linkPortal(LinkPortalSupplierRequest request, StaffUser actor) {
        if (request == null || request.inquiryId() == null) {
            throw new InquiryValidationException("Choose a submitted supplier inquiry.");
        }
        if (trading.supplierIdForPortalInquiry(request.inquiryId()).isPresent()) {
            throw new InquiryValidationException("That supplier is already in the trading directory.");
        }
        TradingCatalogueRepository.PortalParty party = trading.findSubmittedSupplierParty(request.inquiryId())
                .orElseThrow(() -> new InquiryValidationException("Submitted supplier inquiry not found."));
        String company = party.companyName() == null || party.companyName().isBlank()
                ? "Portal supplier"
                : party.companyName().trim();
        UUID id = UUID.randomUUID();
        trading.insertSupplier(
                id,
                "PORTAL",
                party.inquiryId(),
                company,
                emptyToNull(party.contactName()),
                emptyToNull(party.email()),
                emptyToNull(party.phone()),
                emptyToNull(party.websiteUrl()),
                null,
                actor.id()
        );
        int order = 10;
        for (String typeName : trading.productTypeNames(party.inquiryId())) {
            if (typeName == null || typeName.isBlank()) {
                continue;
            }
            String name = typeName.trim();
            if (trading.productNameTaken(id, name, null)) {
                continue;
            }
            trading.insertProduct(UUID.randomUUID(), id, name, false, order);
            order += 10;
        }
        audits.recordUser(party.inquiryId(), "TRADING_SUPPLIER", id, "TRADING_SUPPLIER_LINKED", actor.id(),
                Map.of("sourceKind", "PORTAL"));
        return getSupplier(id);
    }

    @Transactional
    public TradingSupplierDto updateSupplier(UUID id, UpdateTradingSupplierRequest request, StaffUser actor) {
        TradingSupplierDto current = getSupplier(id);
        String company = request == null || request.companyName() == null || request.companyName().isBlank()
                ? current.companyName()
                : requireName(request.companyName(), "Enter a company name.");
        String status = request == null || request.status() == null || request.status().isBlank()
                ? current.status()
                : request.status().trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new InquiryValidationException("Unknown trading supplier status.");
        }
        trading.updateSupplier(
                id,
                company,
                request == null ? current.contactName() : emptyToNull(request.contactName()),
                request == null ? current.email() : emptyToNull(request.email()),
                request == null ? current.phone() : emptyToNull(request.phone()),
                request == null ? current.websiteUrl() : emptyToNull(request.websiteUrl()),
                request == null ? current.notes() : emptyToNull(request.notes()),
                status
        );
        audits.recordUser(current.portalInquiryId(), "TRADING_SUPPLIER", id, "TRADING_SUPPLIER_UPDATED", actor.id(),
                Map.of("status", status, "sourceKind", current.sourceKind()));
        return getSupplier(id);
    }

    @Transactional
    public TradingSupplierDto deactivateSupplier(UUID id, StaffUser actor) {
        TradingSupplierDto current = getSupplier(id);
        if ("INACTIVE".equals(current.status())) {
            return current;
        }
        trading.updateSupplier(
                id,
                current.companyName(),
                current.contactName(),
                current.email(),
                current.phone(),
                current.websiteUrl(),
                current.notes(),
                "INACTIVE"
        );
        audits.recordUser(current.portalInquiryId(), "TRADING_SUPPLIER", id, "TRADING_SUPPLIER_DEACTIVATED", actor.id(),
                Map.of("sourceKind", current.sourceKind()));
        return getSupplier(id);
    }

    @Transactional
    public TradingProductDto addProduct(UUID supplierId, UpsertTradingProductRequest request, StaffUser actor) {
        getSupplier(supplierId);
        String name = requireName(request == null ? null : request.name(), "Enter a product name.");
        if (trading.productNameTaken(supplierId, name, null)) {
            throw new InquiryValidationException("That product name is already listed for this supplier.");
        }
        boolean listed = request != null && request.listedForBuyers() != null && request.listedForBuyers();
        UUID id = UUID.randomUUID();
        try {
            trading.insertProduct(id, supplierId, name, listed, trading.nextDisplayOrder(supplierId));
        } catch (DataIntegrityViolationException ex) {
            throw new InquiryValidationException("That product name is already listed for this supplier.");
        }
        audits.recordUser(null, "TRADING_PRODUCT", id, "TRADING_PRODUCT_CREATED", actor.id(),
                Map.of("listedForBuyers", listed));
        return trading.findProduct(supplierId, id).orElseThrow();
    }

    @Transactional
    public TradingProductDto updateProduct(
            UUID supplierId,
            UUID productId,
            UpsertTradingProductRequest request,
            StaffUser actor
    ) {
        TradingProductDto current = trading.findProduct(supplierId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trading product not found"));
        String name = request == null || request.name() == null || request.name().isBlank()
                ? current.name()
                : requireName(request.name(), "Enter a product name.");
        if (trading.productNameTaken(supplierId, name, productId)) {
            throw new InquiryValidationException("That product name is already listed for this supplier.");
        }
        boolean listed = request == null || request.listedForBuyers() == null
                ? current.listedForBuyers()
                : request.listedForBuyers();
        trading.updateProduct(productId, name, listed, current.active());
        String event = listed && !current.listedForBuyers()
                ? "TRADING_PRODUCT_LISTED"
                : !listed && current.listedForBuyers()
                ? "TRADING_PRODUCT_UNLISTED"
                : "TRADING_PRODUCT_UPDATED";
        audits.recordUser(null, "TRADING_PRODUCT", productId, event, actor.id(),
                Map.of("listedForBuyers", listed));
        return trading.findProduct(supplierId, productId).orElseThrow();
    }

    @Transactional
    public TradingProductDto deactivateProduct(UUID supplierId, UUID productId, StaffUser actor) {
        TradingProductDto current = trading.findProduct(supplierId, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trading product not found"));
        if (!current.active()) {
            return current;
        }
        trading.updateProduct(productId, current.name(), false, false);
        audits.recordUser(null, "TRADING_PRODUCT", productId, "TRADING_PRODUCT_DEACTIVATED", actor.id(),
                Map.of("listedForBuyers", false));
        return trading.findProduct(supplierId, productId).orElseThrow();
    }

    public void assertSelectable(List<BuyerTradingProductDto> rows) {
        if (rows == null) {
            return;
        }
        for (BuyerTradingProductDto row : rows) {
            if (row == null || row.tradingProductId() == null) {
                throw new InquiryValidationException("Each trading product selection needs a product.");
            }
            if (!trading.isSelectable(row.tradingProductId())) {
                throw new InquiryValidationException(
                        "A selected trading product is not listed for buyers.");
            }
        }
    }

    private String requireName(String raw, String message) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.length() > 255) {
            throw new InquiryValidationException(message);
        }
        return name;
    }

    private String emptyToNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
