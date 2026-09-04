package com.sarv.exhibitionportal.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LocalCardScanEngineTest {

    @Test
    void parsesVcardIntoContactProposals() {
        String vcard = """
                BEGIN:VCARD
                VERSION:3.0
                FN:Priya Sharma
                N:Sharma;Priya;;;
                ORG:Example Pharma
                TITLE:BD Manager
                EMAIL:priya@example.com
                TEL:+919876543210
                ADR:;;Street;Mumbai;;;;
                END:VCARD
                """;
        List<CardScanResult.ProposedField> fields = LocalCardScanEngine.parseContactProposals(vcard);
        assertThat(fields).extracting(CardScanResult.ProposedField::fieldKey)
                .contains("full_name", "work_email", "mobile_number", "country_code", "company_name", "job_title", "location_from_card");
        assertThat(fields).anyMatch(f -> f.fieldKey().equals("full_name") && f.value().equals("Priya Sharma"));
        assertThat(fields).anyMatch(f -> f.fieldKey().equals("work_email") && f.value().equals("priya@example.com"));
        assertThat(fields).anyMatch(f -> f.fieldKey().equals("country_code") && f.value().equals("+91"));
        assertThat(fields).anyMatch(f -> f.fieldKey().equals("mobile_number") && f.value().equals("9876543210"));
        assertThat(fields).anyMatch(f -> f.fieldKey().equals("location_from_card") && f.value().equals("Mumbai"));
    }

    @Test
    void parsesMailtoAsEmailOnly() {
        List<CardScanResult.ProposedField> fields =
                LocalCardScanEngine.parseContactProposals("mailto:buyer@sarv.example");
        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).fieldKey()).isEqualTo("work_email");
        assertThat(fields.get(0).value()).isEqualTo("buyer@sarv.example");
    }
}
