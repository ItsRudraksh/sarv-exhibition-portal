package com.sarv.exhibitionportal.inquiry;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
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
    void supplierSubmitRequiresWebsiteOrCatalogue() {
        InquiryDraftDto draft = draft("SUPPLIER", new SupplierDto("Acme APIs", "", "", "", null),
                new BuyerDto("", "", specs()));
        assertThrows(InquiryValidationException.class, () -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void supplierSubmitAcceptsWebsiteWithoutCatalogue() {
        InquiryDraftDto draft = draft("SUPPLIER",
                new SupplierDto("Acme APIs", "https://acme.example", "", "", null),
                new BuyerDto("", "", specs()));
        assertDoesNotThrow(() -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void supplierSubmitAcceptsCatalogueWithoutWebsite() {
        InquiryDraftDto draft = draft("SUPPLIER",
                new SupplierDto("Acme APIs", "", "", "", new CardFileDto("list.pdf", 12L, "application/pdf")),
                new BuyerDto("", "", specs()));
        assertDoesNotThrow(() -> InquiryRules.assertCanSubmit(draft));
    }

    @Test
    void draftWithoutRouteCannotSubmit() {
        InquiryDraftDto draft = draft(null, new SupplierDto("Acme APIs", "https://acme.example", "", "", null),
                new BuyerDto("Need", "", specs()));
        assertThrows(InquiryValidationException.class, () -> InquiryRules.assertCanSubmit(draft));
    }

    private static BuyerSpecificationsDto specs() {
        return new BuyerSpecificationsDto("", "", "", "", "");
    }

    private static InquiryDraftDto draft(String route, SupplierDto supplier, BuyerDto buyer) {
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
                List.of(UUID.fromString("a1000000-0000-4000-8000-000000000001")),
                List.of(UUID.fromString("a2000000-0000-4000-8000-000000000003")),
                buyer,
                true,
                null,
                "POC-TEST"
        );
    }
}
