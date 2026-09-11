package com.sarv.exhibitionportal.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** OCR text to contact proposals. */
class CardTextParserTest {

    @Test
    void parsesPrintedCardStyleText() {
        String text = """
                Priya Sharma
                BD Manager
                Example Pharma Labs
                priya.sharma@example.com
                +91 98765 43210
                """;
        List<CardScanResult.ProposedField> fields = CardTextParser.parse(text);
        assertThat(fields).extracting(CardScanResult.ProposedField::fieldKey)
                .contains("full_name", "work_email", "mobile_number", "country_code", "company_name", "job_title");
        assertThat(fields).anyMatch(f -> f.fieldKey().equals("full_name") && f.value().equals("Priya Sharma"));
        assertThat(fields).anyMatch(f -> f.fieldKey().equals("work_email") && f.value().equals("priya.sharma@example.com"));
        assertThat(fields).anyMatch(f -> f.fieldKey().equals("mobile_number") && f.value().equals("9876543210"));
    }
}
