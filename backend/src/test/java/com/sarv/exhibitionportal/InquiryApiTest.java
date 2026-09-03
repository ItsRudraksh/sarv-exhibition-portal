package com.sarv.exhibitionportal;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.DepartmentDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class InquiryApiTest extends MysqlSpringBootTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void createsDraftBeforeRouteChoice() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.route()).isNull();
        assertThat(created.lifecycleState()).isEqualTo("DRAFT");
        assertThat(created.referenceCode()).startsWith("POC-");
    }

    @Test
    void buyerSubmitSucceedsWithoutCompany() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        InquiryDraftDto ready = buyerDraft(created.id(), created.referenceCode());
        ResponseEntity<InquiryDraftDto> submitted = rest.postForEntity(
                "/api/v1/inquiries/" + created.id() + "/submit",
                ready,
                InquiryDraftDto.class);
        assertThat(submitted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(submitted.getBody()).isNotNull();
        assertThat(submitted.getBody().lifecycleState()).isEqualTo("SUBMITTED");
        assertThat(submitted.getBody().supplier().companyName()).isBlank();
        assertThat(submitted.getBody().referenceCode()).isEqualTo(created.referenceCode());
    }

    @Test
    void supplierSubmitRejectedWithoutWebsiteOrCatalogue() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        InquiryDraftDto ready = supplierDraft(created.id(), created.referenceCode(), "");
        ResponseEntity<Map<String, String>> response = rest.exchange(
                "/api/v1/inquiries/" + created.id() + "/submit",
                HttpMethod.POST,
                new HttpEntity<>(ready),
                new ParameterizedTypeReference<>() {});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).contains("catalogue");
    }

    @Test
    void supplierSubmitWithWebsitePersists() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        InquiryDraftDto ready = supplierDraft(
                created.id(), created.referenceCode(), "https://supplier.example");
        InquiryDraftDto submitted = rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/submit",
                ready,
                InquiryDraftDto.class);
        assertThat(submitted).isNotNull();
        assertThat(submitted.lifecycleState()).isEqualTo("SUBMITTED");
        InquiryDraftDto reloaded = rest.getForObject(
                "/api/v1/inquiries/" + created.id(), InquiryDraftDto.class);
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.lifecycleState()).isEqualTo("SUBMITTED");
        assertThat(reloaded.supplier().websiteUrl()).isEqualTo("https://supplier.example");
        assertThat(reloaded.departmentIds()).isNotEmpty();
        assertThat(reloaded.productTypeIds()).isNotEmpty();
    }

    @Test
    void taxonomyIsSeeded() {
        ResponseEntity<DepartmentDto[]> departments = rest.getForEntity(
                "/api/v1/taxonomy/departments", DepartmentDto[].class);
        assertThat(departments.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(departments.getBody()).isNotEmpty();
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

    private static InquiryDraftDto supplierDraft(UUID id, String reference, String website) {
        return new InquiryDraftDto(
                id, "DRAFT", "supplier-review", "SUPPLIER", "EXHIBITION_QR",
                null, null, null,
                new ContactDto("Asha Rao", "asha@example.com", "+91", "9876543210"),
                new SupplierDto("Himalaya Intermediates", website, "", "", null),
                List.of(UUID.fromString("10000000-0000-4000-8000-000000000001")),
                List.of(UUID.fromString("20000000-0000-4000-8000-000000000003")),
                new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", "")),
                true, null, reference);
    }
}
