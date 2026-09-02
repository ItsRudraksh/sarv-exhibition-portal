package com.sarv.exhibitionportal.fileasset;

import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import java.util.Locale;
import java.util.Set;

/**
 * Type/size allowlist plus magic-byte check. This is not an antivirus product.
 * A failed check must not delete a file that was already written to storage.
 */
public final class FileContentRules {

    public static final Set<String> CARD_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    public static final Set<String> CATALOGUE_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp");

    private FileContentRules() {}

    public static String assertDeclaredAllowlist(
            String purpose,
            String filename,
            String declaredType,
            int byteLength,
            long maxBytes
    ) {
        if (byteLength <= 0) {
            throw new InquiryValidationException("The file is empty.");
        }
        if (byteLength > maxBytes) {
            throw new InquiryValidationException("That file is larger than the allowed size.");
        }
        String mediaType = normalizeType(declaredType, filename);
        if (!allowedTypes(purpose).contains(mediaType)) {
            throw new InquiryValidationException(
                    "Use a JPEG, PNG, or WebP image"
                            + ("CATALOGUE_ORIGINAL".equals(purpose) ? ", or a PDF." : "."));
        }
        return mediaType;
    }

    public static void assertContentsMatch(String purpose, String declaredType, byte[] bytes) {
        String sniffed = sniff(bytes);
        Set<String> allowed = allowedTypes(purpose);
        if (sniffed == null || !allowed.contains(sniffed)) {
            throw new InquiryValidationException("The file contents do not match an allowed type.");
        }
        if (!sniffed.equals(declaredType) && !(isJpeg(declaredType) && isJpeg(sniffed))) {
            throw new InquiryValidationException("The file contents do not match the declared type.");
        }
    }

    public static Set<String> allowedTypes(String purpose) {
        return "BUSINESS_CARD".equals(purpose) ? CARD_TYPES : CATALOGUE_TYPES;
    }

    public static String normalizeType(String declaredType, String filename) {
        if (declaredType != null && !declaredType.isBlank()) {
            String type = declaredType.toLowerCase(Locale.ROOT).split(";")[0].trim();
            if ("image/jpg".equals(type)) {
                return "image/jpeg";
            }
            return type;
        }
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".pdf")) return "application/pdf";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    public static String sniff(byte[] bytes) {
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47) {
            return "image/png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P') {
            return "image/webp";
        }
        if (bytes.length >= 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F'
                && bytes[4] == '-') {
            return "application/pdf";
        }
        return null;
    }

    public static String catalogueFormat(String mediaType) {
        return "application/pdf".equals(mediaType) ? "PDF" : "IMAGES";
    }

    private static boolean isJpeg(String type) {
        return "image/jpeg".equals(type);
    }
}
