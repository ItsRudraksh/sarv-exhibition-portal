package com.sarv.exhibitionportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerProductDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.BuyerTradingProductDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.CreateOfflineSupplierRequest;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.LinkPortalSupplierRequest;
import com.sarv.exhibitionportal.api.dto.PortalSupplierCandidateDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import com.sarv.exhibitionportal.api.dto.TradingProductDto;
import com.sarv.exhibitionportal.api.dto.TradingSupplierDto;
import com.sarv.exhibitionportal.api.dto.UpsertTradingProductRequest;
import com.sarv.exhibitionportal.audit.AuditService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Buyer list union and admin listed_for_buyers tagging. */
class TradingCatalogueApiTest extends MysqlSpringBootTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AuditService audits;

    @Test
    void reviewerCannotManageTradingCatalogue() {
        ResponseEntity<String> forbidden = reviewer().getForEntity("/api/v1/staff/trading-suppliers", String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void offlineSupplierProductCanBeListedForBuyers() {
        CreateOfflineSupplierRequest create = new CreateOfflineSupplierRequest(
                "Himalaya Traders", "Ravi Kumar", "ravi@traders.example", "+91 98000 00000",
                "https://traders.example", "Offline stall contact");
        ResponseEntity<TradingSupplierDto> created = admin().postForEntity(
                "/api/v1/staff/trading-suppliers", create, TradingSupplierDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        UUID supplierId = created.getBody().id();
        assertThat(created.getBody().sourceKind()).isEqualTo("OFFLINE");
        assertThat(audits.countByEntity("TRADING_SUPPLIER", supplierId, "TRADING_SUPPLIER_CREATED")).isEqualTo(1);

        ResponseEntity<TradingProductDto> product = admin().postForEntity(
                "/api/v1/staff/trading-suppliers/" + supplierId + "/products",
                new UpsertTradingProductRequest("Traded Colchicine", true),
                TradingProductDto.class);
        assertThat(product.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(product.getBody()).isNotNull();
        assertThat(product.getBody().listedForBuyers()).isTrue();

        ResponseEntity<BuyerProductDto[]> catalogue = rest.getForEntity(
                "/api/v1/buyer-products", BuyerProductDto[].class);
        assertThat(catalogue.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(catalogue.getBody()).isNotNull();
        assertThat(catalogue.getBody())
                .extracting(BuyerProductDto::name)
                .contains("Traded Colchicine");
        assertThat(catalogue.getBody())
                .filteredOn(row -> "Traded Colchicine".equals(row.name()))
                .extracting(BuyerProductDto::sourceKind)
                .containsExactly("OFFLINE_SUPPLIER");
    }

    @Test
    void unlistedTradingProductIsHiddenFromBuyers() {
        TradingSupplierDto supplier = admin().postForObject(
                "/api/v1/staff/trading-suppliers",
                new CreateOfflineSupplierRequest("Quiet Co", null, null, null, null, null),
                TradingSupplierDto.class);
        assertThat(supplier).isNotNull();
        TradingProductDto hidden = admin().postForObject(
                "/api/v1/staff/trading-suppliers/" + supplier.id() + "/products",
                new UpsertTradingProductRequest("Hidden Extract", false),
                TradingProductDto.class);
        assertThat(hidden).isNotNull();
        assertThat(hidden.listedForBuyers()).isFalse();

        BuyerProductDto[] catalogue = rest.getForObject("/api/v1/buyer-products", BuyerProductDto[].class);
        assertThat(catalogue).extracting(BuyerProductDto::name).doesNotContain("Hidden Extract");
    }

    @Test
    void portalSupplierCanBeLinkedAndTagged() {
        UUID inquiryId = submitSupplier();
        PortalSupplierCandidateDto[] candidates = admin().getForObject(
                "/api/v1/staff/trading-suppliers/portal-candidates", PortalSupplierCandidateDto[].class);
        assertThat(candidates).extracting(PortalSupplierCandidateDto::inquiryId).contains(inquiryId);

        TradingSupplierDto linked = admin().postForObject(
                "/api/v1/staff/trading-suppliers/from-portal",
                new LinkPortalSupplierRequest(inquiryId),
                TradingSupplierDto.class);
        assertThat(linked).isNotNull();
        assertThat(linked.sourceKind()).isEqualTo("PORTAL");
        assertThat(linked.portalInquiryId()).isEqualTo(inquiryId);
        assertThat(linked.products()).isNotEmpty();
        assertThat(linked.products()).allMatch(row -> !row.listedForBuyers());

        TradingProductDto first = linked.products().get(0);
        ResponseEntity<TradingProductDto> tagged = admin().exchange(
                "/api/v1/staff/trading-suppliers/" + linked.id() + "/products/" + first.id(),
                HttpMethod.PUT,
                new HttpEntity<>(new UpsertTradingProductRequest(first.name(), true)),
                TradingProductDto.class);
        assertThat(tagged.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tagged.getBody()).isNotNull();
        assertThat(tagged.getBody().listedForBuyers()).isTrue();

        BuyerProductDto[] catalogue = rest.getForObject("/api/v1/buyer-products", BuyerProductDto[].class);
        assertThat(catalogue).extracting(BuyerProductDto::name).contains(first.name());
    }

    @Test
    void buyerDraftCanSelectListedTradingProduct() {
        TradingSupplierDto supplier = admin().postForObject(
                "/api/v1/staff/trading-suppliers",
                new CreateOfflineSupplierRequest("Select Co", null, null, null, null, null),
                TradingSupplierDto.class);
        TradingProductDto product = admin().postForObject(
                "/api/v1/staff/trading-suppliers/" + supplier.id() + "/products",
                new UpsertTradingProductRequest("Selectable API", true),
                TradingProductDto.class);
        assertThat(product).isNotNull();

        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        InquiryDraftDto ready = buyerDraft(created.id(), created.referenceCode(), product.id());
        InquiryDraftDto submitted = rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/submit", ready, InquiryDraftDto.class);
        assertThat(submitted).isNotNull();
        assertThat(submitted.buyer().tradingProducts()).hasSize(1);
        assertThat(submitted.buyer().tradingProducts().get(0).tradingProductId()).isEqualTo(product.id());
        assertThat(submitted.buyer().tradingProducts().get(0).quantity()).isEqualTo("10 kg");
    }

    private UUID submitSupplier() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        InquiryDraftDto ready = supplierDraft(created.id(), created.referenceCode());
        InquiryDraftDto submitted = rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/submit", ready, InquiryDraftDto.class);
        assertThat(submitted).isNotNull();
        return submitted.id();
    }

    private TestRestTemplate admin() {
        return rest.withBasicAuth("admin@sarv.local", "poc-staff");
    }

    private TestRestTemplate reviewer() {
        return rest.withBasicAuth("reviewer@sarv.local", "poc-staff");
    }

    private static InquiryDraftDto emptyDraft() {
        return new InquiryDraftDto(
                null, "DRAFT", "card-capture", null, "EXHIBITION_QR",
                null, null, null,
                new ContactDto("", "", "+91", ""),
                new SupplierDto("", "", "", "", null),
                List.of(), List.of(),
                new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", "")),
                false, null, null);
    }

    private static InquiryDraftDto buyerDraft(UUID id, String reference, UUID tradingProductId) {
        return new InquiryDraftDto(
                id, "DRAFT", "buyer-review", "PURCHASE", "EXHIBITION_QR",
                null, null, null,
                new ContactDto("Asha Rao", "asha@example.com", "+91", "9876543210"),
                new SupplierDto("", "", "", "", null),
                List.of(), List.of(),
                new BuyerDto(
                        "Selectable API",
                        "",
                        new BuyerSpecificationsDto("", "", "", "", ""),
                        List.of(),
                        List.of(new BuyerTradingProductDto(tradingProductId, "10 kg"))),
                true, null, reference);
    }

    private static InquiryDraftDto supplierDraft(UUID id, String reference) {
        return new InquiryDraftDto(
                id, "DRAFT", "supplier-review", "SUPPLIER", "EXHIBITION_QR",
                null, null, null,
                new ContactDto("Asha Rao", "asha@example.com", "+91", "9876543210"),
                new SupplierDto("Himalaya Intermediates", "https://supplier.example", "", "", null, "Phyto extracts"),
                List.of(UUID.fromString("a1000000-0000-4000-8000-000000000001")),
                List.of(UUID.fromString("a2000000-0000-4000-8000-000000000003")),
                new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", "")),
                true, null, reference);
    }
}
