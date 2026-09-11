package com.sarv.exhibitionportal.inquiry;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.CardFileDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import java.util.List;
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
        boolean otherCategory = supplier.otherCategory();
        boolean otherProductType = supplier.otherProductType();
        if (otherCategory && blank(supplier.otherCategoryDetail())) {
            throw new InquiryValidationException("Describe the other category.");
        }
        if (otherProductType && blank(supplier.otherProductTypeDetail())) {
            throw new InquiryValidationException("Describe the other product type.");
        }
        boolean hasDepartments = draft.departmentIds() != null && !draft.departmentIds().isEmpty();
        boolean hasProductTypes = draft.productTypeIds() != null && !draft.productTypeIds().isEmpty();
        boolean hasNotes = !blank(supplier.capabilityNotes());
        boolean hasCategorySignal = hasDepartments || otherCategory || hasNotes;
        boolean hasTypeSignal = hasProductTypes || otherProductType || hasNotes;
        if (!hasCategorySignal) {
            throw new InquiryValidationException(
                    "Select a category, choose Other, or describe what you can supply.");
        }
        if (!hasTypeSignal) {
            throw new InquiryValidationException(
                    "Select a product type, choose Other, or describe what you can supply.");
        }
        boolean hasWebsite = supplier.websiteUrl() != null && !supplier.websiteUrl().isBlank();
        boolean hasFiles = hasNamedFile(supplier.catalogueFile()) || hasNamedFiles(supplier.attachments());
        if (!hasWebsite && !hasFiles) {
            throw new InquiryValidationException(
                    "Add a supporting file or a website URL — at least one is required.");
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
        if (buyer == null) {
            throw new InquiryValidationException("Describe the product or requirement to continue.");
        }
        if (buyer.otherProduct() && blank(buyer.otherProductDetail())) {
            throw new InquiryValidationException("Describe the other product.");
        }
        boolean hasOther = buyer.otherProduct() && !blank(buyer.otherProductDetail());
        if (blank(buyer.requirement()) && !hasOther) {
            throw new InquiryValidationException("Describe the product or requirement to continue.");
        }
        if (buyer.finishedGoods() != null) {
            for (var row : buyer.finishedGoods()) {
                if (row == null || row.finishedGoodId() == null) {
                    throw new InquiryValidationException("Each finished good selection needs a product.");
                }
                if (blank(row.quantity())) {
                    throw new InquiryValidationException(
                            "Enter a quantity for each selected product.");
                }
            }
        }
        if (buyer.tradingProducts() != null) {
            for (var row : buyer.tradingProducts()) {
                if (row == null || row.tradingProductId() == null) {
                    throw new InquiryValidationException("Each trading product selection needs a product.");
                }
                if (blank(row.quantity())) {
                    throw new InquiryValidationException(
                            "Enter a quantity for each selected product.");
                }
            }
        }
    }

    private static boolean hasNamedFiles(List<CardFileDto> files) {
        if (files == null || files.isEmpty()) {
            return false;
        }
        for (CardFileDto file : files) {
            if (hasNamedFile(file)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasNamedFile(CardFileDto file) {
        return file != null && file.name() != null && !file.name().isBlank();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
