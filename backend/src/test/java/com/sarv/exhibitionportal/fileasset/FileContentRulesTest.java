package com.sarv.exhibitionportal.fileasset;

import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileContentRulesTest {

    private static final byte[] JPEG = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x10, 0x11};
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    private static final byte[] PDF = "%PDF-1.4 sample".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    @Test
    void acceptsJpegCard() {
        String type = FileContentRules.assertDeclaredAllowlist(
                "BUSINESS_CARD", "card.jpg", "image/jpeg", JPEG.length, 10_000);
        assertEquals("image/jpeg", type);
        assertDoesNotThrow(() -> FileContentRules.assertContentsMatch("BUSINESS_CARD", type, JPEG));
    }

    @Test
    void acceptsPngAndPdfCatalogue() {
        assertDoesNotThrow(() -> FileContentRules.assertContentsMatch(
                "CATALOGUE_ORIGINAL", "image/png", PNG));
        assertDoesNotThrow(() -> FileContentRules.assertContentsMatch(
                "CATALOGUE_ORIGINAL", "application/pdf", PDF));
    }

    @Test
    void rejectsMismatchedContentsAfterUpload() {
        byte[] exe = new byte[] {'M', 'Z', 0x00, 0x00, 0x00};
        String type = FileContentRules.assertDeclaredAllowlist(
                "BUSINESS_CARD", "card.jpg", "image/jpeg", exe.length, 10_000);
        assertThrows(InquiryValidationException.class,
                () -> FileContentRules.assertContentsMatch("BUSINESS_CARD", type, exe));
    }

    @Test
    void rejectsPdfAsBusinessCard() {
        assertThrows(InquiryValidationException.class, () -> FileContentRules.assertDeclaredAllowlist(
                "BUSINESS_CARD", "card.pdf", "application/pdf", PDF.length, 10_000));
    }
}
