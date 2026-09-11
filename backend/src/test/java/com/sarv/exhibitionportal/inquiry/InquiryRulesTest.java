package com.sarv.exhibitionportal.inquiry;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerFinishedGoodDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.BuyerTradingProductDto;
import com.sarv.exhibitionportal.api.dto.CardFileDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InquiryRulesTest {

    @Test
    void buyerSubmitDoesNotRequireCompany() {
        InquiryDraftDto draft = draft("PURCHASE", new SupplierDto("", "", "", "", null),
                new BuyerDto("Thiocolchicoside API", "", specs()));
        assertDoesNotThrow(() -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void supplierSubmitRequiresWebsiteOrFile() {
        InquiryDraftDto draft = draft("SUPPLIER", offering("", null),
                new BuyerDto("", "", specs()));
        assertThrows(InquiryValidationException.class, () -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void supplierSubmitAcceptsWebsiteWithoutCatalogue() {
        InquiryDraftDto draft = draft("SUPPLIER",
                offering("https://acme.example", null),
                new BuyerDto("", "", specs()));
        assertDoesNotThrow(() -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void supplierSubmitAcceptsCatalogueWithoutWebsite() {
        InquiryDraftDto draft = draft("SUPPLIER",
                offering("", new CardFileDto("list.pdf", 12L, "application/pdf")),
                new BuyerDto("", "", specs()));
        assertDoesNotThrow(() -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void supplierSubmitAcceptsOtherCategoryWithoutListedDepartments() {
        InquiryDraftDto draft = draft(
                "SUPPLIER",
                new SupplierDto(
                        "Acme APIs",
                        "https://acme.example",
                        "",
                        "",
                        null,
                        true,
                        true,
                        "Custom APIs",
                        "Custom intermediates",
                        "",
                        List.of()),
                new BuyerDto("", "", specs()),
                List.of(),
                List.of());
        assertDoesNotThrow(() -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void supplierSubmitRejectsOtherCategoryWithoutDetails() {
        InquiryDraftDto draft = draft(
                "SUPPLIER",
                new SupplierDto(
                        "Acme APIs",
                        "https://acme.example",
                        "",
                        "",
                        null,
                        true,
                        false,
                        "",
                        "",
                        "Notes alone do not replace Other details",
                        List.of()),
                new BuyerDto("", "", specs()),
                List.of(),
                List.of());
        assertThrows(InquiryValidationException.class, () -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void supplierSubmitAcceptsListedTypesWithoutNotes() {
        InquiryDraftDto draft = draft(
                "SUPPLIER",
                new SupplierDto("Acme APIs", "https://acme.example", "", "", null, ""),
                new BuyerDto("", "", specs()));
        assertDoesNotThrow(() -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void supplierSubmitAcceptsNotesWithoutListedTypes() {
        InquiryDraftDto draft = draft(
                "SUPPLIER",
                new SupplierDto(
                        "Acme APIs",
                        "https://acme.example",
                        "",
                        "",
                        null,
                        "Oncology APIs and custom intermediates"),
                new BuyerDto("", "", specs()),
                List.of(),
                List.of());
        assertDoesNotThrow(() -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void supplierSubmitRequiresOfferingWhenNothingSelected() {
        InquiryDraftDto draft = draft(
                "SUPPLIER",
                new SupplierDto("Acme APIs", "https://acme.example", "", "", null, ""),
                new BuyerDto("", "", specs()),
                List.of(),
                List.of());
        assertThrows(InquiryValidationException.class, () -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void draftWithoutRouteCannotSubmit() {
        InquiryDraftDto draft = draft(null, offering("https://acme.example", null),
                new BuyerDto("Need", "", specs()));
        assertThrows(InquiryValidationException.class, () -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void buyerSubmitRequiresQuantityForEachFinishedGood() {
        InquiryDraftDto draft = draft(
                "PURCHASE",
                new SupplierDto("", "", "", "", null),
                new BuyerDto(
                        "Need",
                        "",
                        specs(),
                        List.of(new BuyerFinishedGoodDto(UUID.randomUUID(), ""))));
        assertThrows(InquiryValidationException.class, () -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void buyerSubmitAcceptsFinishedGoodWithQuantity() {
        InquiryDraftDto draft = draft(
                "PURCHASE",
                new SupplierDto("", "", "", "", null),
                new BuyerDto(
                        "Need",
                        "",
                        specs(),
                        List.of(new BuyerFinishedGoodDto(UUID.randomUUID(), "25 kg"))));
        assertDoesNotThrow(() -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void buyerSubmitRequiresQuantityForEachTradingProduct() {
        InquiryDraftDto draft = draft(
                "PURCHASE",
                new SupplierDto("", "", "", "", null),
                new BuyerDto(
                        "Need",
                        "",
                        specs(),
                        List.of(),
                        List.of(new BuyerTradingProductDto(UUID.randomUUID(), ""))));
        assertThrows(InquiryValidationException.class, () -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void buyerSubmitAcceptsOtherProductWithoutRequirementText() {
        InquiryDraftDto draft = draft(
                "PURCHASE",
                new SupplierDto("", "", "", "", null),
                new BuyerDto(
                        "",
                        "",
                        specs(),
                        List.of(),
                        List.of(),
                        List.of(),
                        true,
                        "Custom extract not in the catalogue"));
        assertDoesNotThrow(() -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void buyerSubmitRejectsOtherProductWithoutDetails() {
        InquiryDraftDto draft = draft(
                "PURCHASE",
                new SupplierDto("", "", "", "", null),
                new BuyerDto(
                        "Need",
                        "",
                        specs(),
                        List.of(),
                        List.of(),
                        List.of(),
                        true,
                        ""));
        assertThrows(InquiryValidationException.class, () -> InquiryRules.assertCanSubmit(draft));
    }

    private static SupplierDto offering(String website, CardFileDto file) {
        return new SupplierDto("Acme APIs", website, "", "", file, "APIs and intermediates");
    }

    private static BuyerSpecificationsDto specs() {
        return new BuyerSpecificationsDto("", "", "", "", "");
    }

    private static InquiryDraftDto draft(String route, SupplierDto supplier, BuyerDto buyer) {
        return draft(
                route,
                supplier,
                buyer,
                List.of(UUID.fromString("a1000000-0000-4000-8000-000000000001")),
                List.of(UUID.fromString("a2000000-0000-4000-8000-000000000003")));
    }

    private static InquiryDraftDto draft(
            String route,
            SupplierDto supplier,
            BuyerDto buyer,
            List<UUID> departments,
            List<UUID> productTypes
    ) {
        return new InquiryDraftDto(
                UUID.randomUUID(),
                "DRAFT",
                "buyer-review",
                route,
                "EXHIBITION_QR",
                null,
                null,
                null,
                new ContactDto("Asha Rao", "asha@example.com", "+91", "9876543210"),
                supplier,
                departments,
                productTypes,
                buyer,
                true,
                null,
                "POC-TEST"
        );
    }
}
