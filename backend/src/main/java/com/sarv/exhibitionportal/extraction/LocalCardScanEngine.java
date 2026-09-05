package com.sarv.exhibitionportal.extraction;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * Local card assist: decode QR with ZXing and, when the payload is a vCard/MECARD/contact URI,
 * propose reviewable contact fields. This is not cloud OCR and does not invent an AI provider.
 */
@Component
public class LocalCardScanEngine {

    static final String PROVIDER = "zxing-qr-v1";
    private static final BigDecimal QR_CONFIDENCE = new BigDecimal("0.900");
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEL_URI = Pattern.compile("(?i)^tel:(.+)$");
    private static final Pattern MAILTO = Pattern.compile("(?i)^mailto:(.+)$");

    public CardScanResult scan(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return CardScanResult.empty(PROVIDER);
        }
        String qrText;
        try {
            qrText = decodeQr(imageBytes);
        } catch (IOException ex) {
            return CardScanResult.empty(PROVIDER);
        }
        if (qrText == null || qrText.isBlank()) {
            return CardScanResult.empty(PROVIDER);
        }
        List<CardScanResult.ProposedField> fields = parseContactProposals(qrText.trim());
        return new CardScanResult(qrText.trim(), fields, PROVIDER);
    }

    private static String decodeQr(byte[] imageBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            return null;
        }
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        try {
            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException ex) {
            return null;
        }
    }

    static List<CardScanResult.ProposedField> parseContactProposals(String payload) {
        List<CardScanResult.ProposedField> fields = new ArrayList<>();
        String upper = payload.toUpperCase(Locale.ROOT);
        if (upper.startsWith("BEGIN:VCARD")) {
            parseVcard(payload, fields);
        } else if (upper.startsWith("MECARD:")) {
            parseMecard(payload, fields);
        } else {
            Matcher mailto = MAILTO.matcher(payload.trim());
            if (mailto.matches()) {
                add(fields, "work_email", mailto.group(1).trim());
            } else {
                Matcher tel = TEL_URI.matcher(payload.trim());
                if (tel.matches()) {
                    addPhone(fields, tel.group(1).trim());
                } else {
                    Matcher email = EMAIL.matcher(payload);
                    if (email.find()) {
                        add(fields, "work_email", email.group());
                    }
                }
            }
        }
        return fields;
    }

    private static void parseVcard(String payload, List<CardScanResult.ProposedField> fields) {
        String fullName = null;
        String org = null;
        String title = null;
        String email = null;
        String tel = null;
        String city = null;
        for (String rawLine : payload.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.toUpperCase(Locale.ROOT).startsWith("BEGIN:")
                    || line.toUpperCase(Locale.ROOT).startsWith("END:")
                    || line.toUpperCase(Locale.ROOT).startsWith("VERSION:")) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String keyPart = line.substring(0, colon);
            String value = line.substring(colon + 1).trim();
            String key = keyPart.split(";", 2)[0].toUpperCase(Locale.ROOT);
            switch (key) {
                case "FN" -> fullName = value;
                case "N" -> {
                    if (fullName == null || fullName.isBlank()) {
                        fullName = fromVcardN(value);
                    }
                }
                case "ORG" -> org = value.split(";", 2)[0].trim();
                case "TITLE" -> title = value;
                case "EMAIL" -> {
                    if (email == null) {
                        email = value;
                    }
                }
                case "TEL" -> {
                    if (tel == null) {
                        tel = value;
                    }
                }
                case "ADR" -> {
                    if (city == null) {
                        city = fromVcardAdr(value);
                    }
                }
                default -> {
                    // ignore other properties
                }
            }
        }
        add(fields, "full_name", fullName);
        add(fields, "work_email", email);
        addPhone(fields, tel);
        add(fields, "company_name", org);
        add(fields, "job_title", title);
        add(fields, "location_from_card", city);
    }

    private static void parseMecard(String payload, List<CardScanResult.ProposedField> fields) {
        String body = payload.substring("MECARD:".length());
        String name = mecardField(body, "N:");
        String tel = mecardField(body, "TEL:");
        String email = mecardField(body, "EMAIL:");
        String org = mecardField(body, "ORG:");
        String note = mecardField(body, "NOTE:");
        add(fields, "full_name", name == null ? null : name.replace(",", " ").trim());
        add(fields, "work_email", email);
        addPhone(fields, tel);
        add(fields, "company_name", org);
        add(fields, "location_from_card", note);
    }

    private static String mecardField(String body, String prefix) {
        int start = indexOfIgnoreCase(body, prefix);
        if (start < 0) {
            return null;
        }
        int valueStart = start + prefix.length();
        int end = body.indexOf(';', valueStart);
        if (end < 0) {
            end = body.length();
        }
        String value = body.substring(valueStart, end).trim();
        return value.isEmpty() ? null : value;
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        return haystack.toUpperCase(Locale.ROOT).indexOf(needle.toUpperCase(Locale.ROOT));
    }

    private static String fromVcardN(String value) {
        String[] parts = value.split(";", -1);
        String family = parts.length > 0 ? parts[0].trim() : "";
        String given = parts.length > 1 ? parts[1].trim() : "";
        return (given + " " + family).trim();
    }

    private static String fromVcardAdr(String value) {
        String[] parts = value.split(";", -1);
        // ADR: PO Box; Extended; Street; Locality; Region; Postal; Country
        if (parts.length > 3 && !parts[3].isBlank()) {
            return parts[3].trim();
        }
        if (parts.length > 6 && !parts[6].isBlank()) {
            return parts[6].trim();
        }
        return null;
    }

    private static void addPhone(List<CardScanResult.ProposedField> fields, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+91") && digits.length() > 3) {
            add(fields, "country_code", "+91");
            add(fields, "mobile_number", digits.substring(3));
            return;
        }
        if (digits.startsWith("+1") && digits.length() > 2) {
            add(fields, "country_code", "+1");
            add(fields, "mobile_number", digits.substring(2));
            return;
        }
        if (digits.startsWith("+44") && digits.length() > 3) {
            add(fields, "country_code", "+44");
            add(fields, "mobile_number", digits.substring(3));
            return;
        }
        if (digits.startsWith("+")) {
            add(fields, "mobile_number", digits);
            return;
        }
        add(fields, "mobile_number", digits.isBlank() ? raw.trim() : digits);
    }

    private static void add(List<CardScanResult.ProposedField> fields, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.length() > 500) {
            trimmed = trimmed.substring(0, 500);
        }
        for (CardScanResult.ProposedField existing : fields) {
            if (existing.fieldKey().equals(key)) {
                return;
            }
        }
        fields.add(new CardScanResult.ProposedField(key, trimmed, QR_CONFIDENCE));
    }
}
