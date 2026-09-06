package com.sarv.exhibitionportal.extraction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses free text (OCR output or plain notes) into reviewable contact proposals.
 * Shared by local OCR assist — not a cloud AI provider.
 */
public final class CardTextParser {

    private static final BigDecimal OCR_CONFIDENCE = new BigDecimal("0.650");
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile(
            "(?:\\+|00)?[0-9][0-9\\s().-]{7,18}[0-9]");
    private static final Pattern TITLE_HINT = Pattern.compile(
            "(?i)\\b(manager|director|officer|engineer|executive|head|lead|ceo|cto|cfo|md|vp|president)\\b");

    private CardTextParser() {}

    public static List<CardScanResult.ProposedField> parse(String rawText) {
        List<CardScanResult.ProposedField> fields = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) {
            return fields;
        }
        String text = rawText.replace('\u00a0', ' ').trim();
        Matcher email = EMAIL.matcher(text);
        if (email.find()) {
            add(fields, "work_email", email.group());
        }
        Matcher phone = PHONE.matcher(text);
        while (phone.find()) {
            String candidate = phone.group();
            if (candidate.contains("@")) {
                continue;
            }
            String digits = candidate.replaceAll("[^0-9+]", "");
            if (digits.replace("+", "").length() < 8) {
                continue;
            }
            addPhone(fields, candidate);
            break;
        }
        String[] lines = text.split("\\R");
        String fullName = null;
        String company = null;
        String title = null;
        for (String rawLine : lines) {
            String line = rawLine.trim().replaceAll("\\s+", " ");
            if (line.isEmpty() || line.length() > 80) {
                continue;
            }
            if (EMAIL.matcher(line).find() || PHONE.matcher(line).find()) {
                continue;
            }
            if (line.matches("(?i)^(www\\.|https?://|tel[:\\s]|email[:\\s]|ph[:\\s]|mob[:\\s]).*")) {
                continue;
            }
            if (TITLE_HINT.matcher(line).find() && title == null) {
                title = line;
                continue;
            }
            if (looksLikePersonName(line) && fullName == null) {
                fullName = line;
                continue;
            }
            if (looksLikeCompany(line) && company == null) {
                company = line;
            }
        }
        add(fields, "full_name", fullName);
        add(fields, "company_name", company);
        add(fields, "job_title", title);
        return fields;
    }

    private static boolean looksLikePersonName(String line) {
        if (!line.matches("[A-Za-z][A-Za-z .'-]{1,60}")) {
            return false;
        }
        String[] parts = line.split("\\s+");
        return parts.length >= 2 && parts.length <= 4;
    }

    private static boolean looksLikeCompany(String line) {
        if (line.length() < 3) {
            return false;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("ltd") || lower.contains("limited") || lower.contains("pvt")
                || lower.contains("inc") || lower.contains("llc") || lower.contains("pharma")
                || lower.contains("labs") || lower.contains("bio") || lower.contains("corp")) {
            return true;
        }
        return line.equals(line.toUpperCase(Locale.ROOT)) && line.matches("[A-Z0-9 &.,'-]{3,60}");
    }

    private static void addPhone(List<CardScanResult.ProposedField> fields, String raw) {
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("00")) {
            digits = "+" + digits.substring(2);
        }
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
        if (digits.length() == 10) {
            add(fields, "country_code", "+91");
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
        fields.add(new CardScanResult.ProposedField(key, trimmed, OCR_CONFIDENCE));
    }
}
