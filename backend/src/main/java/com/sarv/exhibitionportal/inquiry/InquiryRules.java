package com.sarv.exhibitionportal.inquiry;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import java.util.regex.Pattern;

public final class InquiryRules {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private InquiryRules() {}

    public static void assertContact(ContactDto contact) {
        if (contact == null || blank(contact.fullName())) {
            throw new InquiryValidationException("Full name is required.");
        }
        if (blank(contact.workEmail()) || !EMAIL.matcher(contact.workEmail().trim()).matches()) {
            throw new InquiryValidationException("Enter a valid work email address.");
        }
        if (blank(contact.countryCode())) {
            throw new InquiryValidationException("Country code is required.");
        }
        String digits = contact.mobileNumber() == null ? "" : contact.mobileNumber().replaceAll("\\D", "");
        if (digits.length() < 6) {
            throw new InquiryValidationException("Enter a valid mobile number.");
        }
    }

    public static void assertCanSubmit(InquiryDraftDto draft) {
        if (draft == null) {
            throw new InquiryValidationException("Inquiry is required.");
        }
        if (!draft.contactConfirmed()) {
            throw new InquiryValidationException("Confirm contact details before submitting.");
        }
        assertContact(draft.contact());
        if (draft.route() == null || draft.route().isBlank()) {
            throw new InquiryValidationException("Choose I want to sell or I want to buy before submitting.");
        }
        if ("SUPPLIER".equals(draft.route())) {
            assertSupplierSubmit(draft);
        } else if ("PURCHASE".equals(draft.route())) {
            assertBuyerSubmit(draft.buyer());
        } else {
            throw new InquiryValidationException("Unknown inquiry route.");
        }
    }

    public static void assertSupplierSubmit(InquiryDraftDto draft) {
        SupplierDto supplier = draft.supplier();
        if (supplier == null || blank(supplier.companyName())) {
            throw new InquiryValidationException("Company name is required.");
        }
        if (draft.departmentIds() == null || draft.departmentIds().isEmpty()) {
            throw new InquiryValidationException("Select at least one department.");
        }
        if (draft.productTypeIds() == null || draft.productTypeIds().isEmpty()) {
            throw new InquiryValidationException("Select at least one product type.");
        }
        boolean hasWebsite = supplier.websiteUrl() != null && !supplier.websiteUrl().isBlank();
        boolean hasCatalogue = supplier.catalogueFile() != null
                && supplier.catalogueFile().name() != null
                && !supplier.catalogueFile().name().isBlank();
        if (!hasWebsite && !hasCatalogue) {
            throw new InquiryValidationException(
                    "Add a catalogue file or a website URL — at least one is required.");
        }
        if (hasWebsite) {
            String url = supplier.websiteUrl().startsWith("http")
                    ? supplier.websiteUrl()
                    : "https://" + supplier.websiteUrl();
            try {
                java.net.URI.create(url).toURL();
            } catch (Exception ex) {
                throw new InquiryValidationException("Enter a valid website URL.");
            }
        }
    }

    public static void assertBuyerSubmit(BuyerDto buyer) {
        if (buyer == null || blank(buyer.requirement())) {
            throw new InquiryValidationException("Describe the product or requirement to continue.");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
