package com.sarv.exhibitionportal;

import com.sarv.exhibitionportal.api.dto.DepartmentDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.ProductTypeDto;
import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class TaxonomyApiTest extends MysqlSpringBootTest {

    private static final UUID DEPT_PHYTO =
            UUID.fromString("a1000000-0000-4000-8000-000000000001");
    private static final UUID TYPE_PHYTO_EXTRACTS =
            UUID.fromString("a2000000-0000-4000-8000-000000000003");

    @Autowired
    private TestRestTemplate rest;

    @Test
    void departmentsReturnOnlyActiveBusinessRows() {
        ResponseEntity<DepartmentDto[]> response = rest.getForEntity(
                "/api/v1/taxonomy/departments", DepartmentDto[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<DepartmentDto> departments = Arrays.asList(response.getBody());
        assertThat(departments).extracting(DepartmentDto::code)
                .contains("phytochemicals", "oncology_apis")
                .noneMatch(code -> code.startsWith("poc_"));
        assertThat(departments).extracting(DepartmentDto::id)
                .contains(DEPT_PHYTO)
                .doesNotContain(UUID.fromString("10000000-0000-4000-8000-000000000001"));
    }

    @Test
    void productTypesFilterByDepartmentAndExcludeArchivedPoc() {
        ResponseEntity<ProductTypeDto[]> all = rest.getForEntity(
                "/api/v1/taxonomy/product-types", ProductTypeDto[].class);
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(all.getBody()).isNotNull();
        assertThat(all.getBody()).extracting(ProductTypeDto::code)
                .contains("phyto_extracts")
                .noneMatch(code -> code.startsWith("poc_"));

        ResponseEntity<ProductTypeDto[]> filtered = rest.getForEntity(
                "/api/v1/taxonomy/product-types?departmentIds=" + DEPT_PHYTO,
                ProductTypeDto[].class);
        assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filtered.getBody()).isNotNull();
        assertThat(filtered.getBody()).isNotEmpty();
        assertThat(filtered.getBody()).allSatisfy(pt ->
                assertThat(pt.departmentIds()).contains(DEPT_PHYTO));
        assertThat(filtered.getBody()).extracting(ProductTypeDto::id)
                .contains(TYPE_PHYTO_EXTRACTS);
    }

    @Test
    void supplierSubmitAcceptsMappedBusinessTaxonomy() {
        InquiryDraftDto created = rest.postForObject(
                "/api/v1/inquiries", emptyDraft(), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        InquiryDraftDto ready = new InquiryDraftDto(
                created.id(), "DRAFT", "supplier-review", "SUPPLIER", "EXHIBITION_QR",
                null, null, null,
                new ContactDto("Asha Rao", "asha@example.com", "+91", "9876543210"),
                new SupplierDto("Himalaya Intermediates", "https://supplier.example", "", "", null),
                List.of(DEPT_PHYTO),
                List.of(TYPE_PHYTO_EXTRACTS),
                new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", "")),
                true, null, created.referenceCode());
        InquiryDraftDto submitted = rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/submit",
                ready,
                InquiryDraftDto.class);
        assertThat(submitted).isNotNull();
        assertThat(submitted.lifecycleState()).isEqualTo("SUBMITTED");
        assertThat(submitted.departmentIds()).containsExactly(DEPT_PHYTO);
        assertThat(submitted.productTypeIds()).containsExactly(TYPE_PHYTO_EXTRACTS);
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
}
