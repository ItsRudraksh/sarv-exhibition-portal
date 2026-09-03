package com.sarv.exhibitionportal;

import com.sarv.exhibitionportal.api.StaffController;
import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerLeadDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import com.sarv.exhibitionportal.api.dto.SupplierReviewDto;
import com.sarv.exhibitionportal.outbox.OutboxService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxApiTest extends MysqlSpringBootTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private OutboxService outbox;

    @Test
    void duplicateBuyerSubmitDoesNotDoubleEnqueue() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        InquiryDraftDto ready = buyerDraft(created.id(), created.referenceCode());
        rest.postForObject("/api/v1/inquiries/" + created.id() + "/submit", ready, InquiryDraftDto.class);
        rest.postForObject("/api/v1/inquiries/" + created.id() + "/submit", ready, InquiryDraftDto.class);
        assertThat(outbox.count(created.id(), OutboxService.MARKETING_LEAD)).isEqualTo(1);
        assertThat(outbox.count(created.id(), OutboxService.VENDOR_UPSERT)).isZero();
    }

    @Test
    void supplierSubmitDoesNotEnqueueVendorUpsert() {
        UUID id = submitSupplier();
        assertThat(outbox.count(id, OutboxService.VENDOR_UPSERT)).isZero();
        assertThat(outbox.count(id, OutboxService.MARKETING_LEAD)).isZero();
    }

    @Test
    void addToProductionEnqueuesVendorAndWorkerSucceedsWithoutLiveApi() {
        UUID id = submitSupplier();
        ResponseEntity<SupplierReviewDto> approved = reviewer().postForEntity(
                "/api/v1/staff/suppliers/" + id + "/decisions",
                new StaffController.DecisionRequest("APPROVE", "ok"),
                SupplierReviewDto.class);
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody()).isNotNull();
        assertThat(approved.getBody().productionState()).isEqualTo("QUEUED");
        assertThat(approved.getBody().deliveryState()).isEqualTo("PENDING");
        assertThat(outbox.count(id, OutboxService.VENDOR_UPSERT)).isEqualTo(1);

        assertThat(outbox.processDue()).isGreaterThan(0);
        SupplierReviewDto after = reviewer().getForObject("/api/v1/staff/suppliers/" + id, SupplierReviewDto.class);
        assertThat(after).isNotNull();
        assertThat(after.deliveryState()).isEqualTo("SUCCEEDED");
        assertThat(after.productionState()).isEqualTo("SUCCEEDED");
        InquiryDraftDto source = rest.getForObject("/api/v1/inquiries/" + id, InquiryDraftDto.class);
        assertThat(source).isNotNull();
        assertThat(source.lifecycleState()).isEqualTo("SUBMITTED");
    }

    @Test
    void marketingLeadWorkerDispatchesStubAndKeepsInquiry() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/submit",
                buyerDraft(created.id(), created.referenceCode()),
                InquiryDraftDto.class);
        BuyerLeadDto[] listed = marketing().getForObject("/api/v1/staff/buyers", BuyerLeadDto[].class);
        assertThat(listed).isNotNull();
        BuyerLeadDto queued = java.util.Arrays.stream(listed)
                .filter(row -> created.id().equals(row.id()))
                .findFirst()
                .orElseThrow();
        assertThat(queued.deliveryState()).isEqualTo("PENDING");
        assertThat(queued.leadState()).isEqualTo("QUEUED");

        outbox.processDue();
        BuyerLeadDto[] after = marketing().getForObject("/api/v1/staff/buyers", BuyerLeadDto[].class);
        assertThat(after).isNotNull();
        BuyerLeadDto dispatched = java.util.Arrays.stream(after)
                .filter(row -> created.id().equals(row.id()))
                .findFirst()
                .orElseThrow();
        assertThat(dispatched.deliveryState()).isEqualTo("SUCCEEDED");
        assertThat(dispatched.leadState()).isEqualTo("DISPATCHED");
        assertThat(rest.getForObject("/api/v1/inquiries/" + created.id(), InquiryDraftDto.class).lifecycleState())
                .isEqualTo("SUBMITTED");
    }

    @Test
    void sanitizeRedactsContactFromErrorText() {
        assertThat(OutboxService.sanitize("fail asha@example.com +91 9876543210"))
                .doesNotContain("asha@example.com")
                .doesNotContain("9876543210");
    }

    private UUID submitSupplier() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        InquiryDraftDto submitted = rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/submit",
                supplierDraft(created.id(), created.referenceCode()),
                InquiryDraftDto.class);
        assertThat(submitted).isNotNull();
        return submitted.id();
    }

    private TestRestTemplate reviewer() {
        return rest.withBasicAuth("reviewer@sarv.local", "poc-staff");
    }

    private TestRestTemplate marketing() {
        return rest.withBasicAuth("marketing@sarv.local", "poc-staff");
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

    private static InquiryDraftDto buyerDraft(UUID id, String reference) {
        return new InquiryDraftDto(
                id, "DRAFT", "buyer-review", "PURCHASE", "EXHIBITION_QR",
                null, null, null,
                new ContactDto("Asha Rao", "asha@example.com", "+91", "9876543210"),
                new SupplierDto("", "", "", "", null),
                List.of(), List.of(),
                new BuyerDto("Thiocolchicoside, USP grade", "", new BuyerSpecificationsDto("", "", "", "", "")),
                true, null, reference);
    }

    private static InquiryDraftDto supplierDraft(UUID id, String reference) {
        return new InquiryDraftDto(
                id, "DRAFT", "supplier-review", "SUPPLIER", "EXHIBITION_QR",
                null, null, null,
                new ContactDto("Asha Rao", "asha@example.com", "+91", "9876543210"),
                new SupplierDto("Himalaya Intermediates", "https://supplier.example", "", "", null),
                List.of(UUID.fromString("10000000-0000-4000-8000-000000000001")),
                List.of(UUID.fromString("20000000-0000-4000-8000-000000000003")),
                new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", "")),
                true, null, reference);
    }
}
