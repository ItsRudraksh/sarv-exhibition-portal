package com.sarv.exhibitionportal;

import com.sarv.exhibitionportal.api.StaffController;
import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerLeadDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.ExportJobDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.StaffMeDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import com.sarv.exhibitionportal.api.dto.SupplierReviewDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class StaffReviewApiTest extends MysqlSpringBootTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void visitorSubmitStillWorksWithoutStaffAuth() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        assertThat(created.lifecycleState()).isEqualTo("DRAFT");
    }

    @Test
    void staffMeRequiresAuth() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/staff/me", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void reviewerCanSignIn() {
        StaffMeDto me = reviewer().getForObject("/api/v1/staff/me", StaffMeDto.class);
        assertThat(me).isNotNull();
        assertThat(me.email()).isEqualTo("reviewer@sarv.local");
        assertThat(me.roles()).contains("SUPPLIER_REVIEWER");
    }

    @Test
    void addToProductionRecordsActorAndQueuesProductionWithoutVendorCall() {
        UUID id = submitSupplier();
        ResponseEntity<SupplierReviewDto> approved = reviewer().postForEntity(
                "/api/v1/staff/suppliers/" + id + "/decisions",
                new StaffController.DecisionRequest("APPROVE", "Matches stall conversation"),
                SupplierReviewDto.class);
        assertThat(approved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approved.getBody()).isNotNull();
        assertThat(approved.getBody().reviewState()).isEqualTo("APPROVED");
        assertThat(approved.getBody().productionState()).isEqualTo("QUEUED");
        assertThat(approved.getBody().approvedByUserId())
                .isEqualTo(UUID.fromString("44444444-4444-4444-8444-444444444441"));

        ResponseEntity<String> again = reviewer().postForEntity(
                "/api/v1/staff/suppliers/" + id + "/decisions",
                new StaffController.DecisionRequest("APPROVE", null),
                String.class);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectLeavesProductionNotRequested() {
        UUID id = submitSupplier();
        ResponseEntity<SupplierReviewDto> rejected = reviewer().postForEntity(
                "/api/v1/staff/suppliers/" + id + "/decisions",
                new StaffController.DecisionRequest("REJECT", "Out of scope"),
                SupplierReviewDto.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejected.getBody()).isNotNull();
        assertThat(rejected.getBody().reviewState()).isEqualTo("REJECTED");
        assertThat(rejected.getBody().productionState()).isEqualTo("NOT_REQUESTED");
        assertThat(rejected.getBody().approvedByUserId()).isNull();
    }

    @Test
    void marketingCannotReviewSuppliersButCanExportBuyers() throws Exception {
        UUID buyerId = submitBuyer();
        ResponseEntity<String> forbidden = marketing().getForEntity("/api/v1/staff/suppliers", String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        ResponseEntity<BuyerLeadDto[]> buyers = marketing().getForEntity("/api/v1/staff/buyers", BuyerLeadDto[].class);
        assertThat(buyers.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(buyers.getBody()).isNotEmpty();
        assertThat(buyers.getBody()[0].id()).isEqualTo(buyerId);

        ResponseEntity<ExportJobDto> job = marketing().postForEntity(
                "/api/v1/staff/exports", null, ExportJobDto.class);
        assertThat(job.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(job.getBody()).isNotNull();
        assertThat(job.getBody().state()).isEqualTo("READY");
        assertThat(job.getBody().scope()).isEqualTo("PURCHASE_LEADS");
        assertThat(job.getBody().originalFilename()).endsWith(".xlsx");

        ResponseEntity<byte[]> file = marketing().getForEntity(
                "/api/v1/staff/exports/" + job.getBody().id() + "/file", byte[].class);
        assertThat(file.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(file.getBody()).isNotNull();
        assertThat(file.getBody()[0]).isEqualTo((byte) 'P');
        assertThat(file.getBody()[1]).isEqualTo((byte) 'K');
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                             new java.io.ByteArrayInputStream(file.getBody()))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("reference_code");
            boolean found = false;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var cell = sheet.getRow(i).getCell(5);
                if (cell != null && cell.getStringCellValue().contains("Thiocolchicoside")) {
                    found = true;
                    break;
                }
            }
            assertThat(found).isTrue();
        }
    }

    @Test
    void reviewerCannotCreateExport() {
        ResponseEntity<String> forbidden = reviewer().postForEntity("/api/v1/staff/exports", null, String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
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

    private UUID submitBuyer() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        InquiryDraftDto ready = buyerDraft(created.id(), created.referenceCode());
        InquiryDraftDto submitted = rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/submit", ready, InquiryDraftDto.class);
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
                List.of(UUID.fromString("a1000000-0000-4000-8000-000000000001")),
                List.of(UUID.fromString("a2000000-0000-4000-8000-000000000003")),
                new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", "")),
                true, null, reference);
    }
}
