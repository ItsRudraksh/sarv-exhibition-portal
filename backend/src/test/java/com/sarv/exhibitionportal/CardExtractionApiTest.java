package com.sarv.exhibitionportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sarv.exhibitionportal.api.dto.ExtractionDto;
import com.sarv.exhibitionportal.api.dto.FileAssetDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.audit.AuditService;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

class CardExtractionApiTest extends MysqlSpringBootTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AuditService audits;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void cardUploadWithVcardQrProposesFieldsAndHidesRawPayload() throws Exception {
        String vcard = """
                BEGIN:VCARD
                VERSION:3.0
                FN:Asha Rao
                EMAIL:asha@example.com
                TEL:+919911122233
                ORG:Demo Labs
                END:VCARD
                """.trim();
        byte[] png = qrPng(vcard);

        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", null, InquiryDraftDto.class);
        assertThat(created).isNotNull();
        FileAssetDto asset = upload(created.id(), png);

        ResponseEntity<ExtractionDto> extraction = rest.getForEntity(
                "/api/v1/inquiries/" + created.id() + "/extractions/latest", ExtractionDto.class);
        assertThat(extraction.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(extraction.getBody()).isNotNull();
        assertThat(extraction.getBody().cardQrDetected()).isTrue();
        assertThat(extraction.getBody().fields())
                .extracting(ExtractionDto.ExtractedFieldDto::fieldKey)
                .contains("full_name", "work_email", "mobile_number", "company_name");

        InquiryDraftDto reloaded = rest.getForObject("/api/v1/inquiries/" + created.id(), InquiryDraftDto.class);
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.cardQrPayloadInternal()).isNull();
        assertThat(reloaded.cardFront()).isNotNull();
        assertThat(reloaded.cardFront().assetId()).isEqualTo(asset.id());

        String storedQr = jdbc.sql("""
                        select card_qr_payload_internal from inquiry_ui_state where inquiry_id = :id
                        """)
                .param("id", created.id().toString())
                .query(String.class)
                .single();
        assertThat(storedQr).contains("asha@example.com");

        assertThat(audits.count(created.id(), "CARD_EXTRACTION_COMPLETED")).isEqualTo(1);
    }

    @Test
    void voiceFeatureIsRejected() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", null, InquiryDraftDto.class);
        assertThat(created).isNotNull();
        ResponseEntity<String> response = rest.postForEntity(
                "/api/v1/inquiries/" + created.id() + "/extractions",
                Map.of("feature", "VOICE_INPUT"),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsIgnoringCase("Voice");
    }

    @Test
    void contactConfirmMarksProposalsReviewedWithoutOverwritingVisitorEdits() throws Exception {
        String vcard = """
                BEGIN:VCARD
                VERSION:3.0
                FN:Suggested Name
                EMAIL:suggested@example.com
                TEL:+919900011122
                END:VCARD
                """.trim();
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", null, InquiryDraftDto.class);
        assertThat(created).isNotNull();
        upload(created.id(), qrPng(vcard));

        InquiryDraftDto withContact = new InquiryDraftDto(
                created.id(),
                "DRAFT",
                "contact-confirm",
                null,
                "EXHIBITION_QR",
                created.cardFront(),
                null,
                null,
                new com.sarv.exhibitionportal.api.dto.ContactDto(
                        "Visitor Corrected", "visitor@example.com", "+91", "9900011122"),
                created.supplier(),
                java.util.List.of(),
                java.util.List.of(),
                created.buyer(),
                false,
                null,
                created.referenceCode()
        );
        InquiryDraftDto confirmed = rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/contact",
                withContact,
                InquiryDraftDto.class);
        assertThat(confirmed).isNotNull();
        assertThat(confirmed.contact().fullName()).isEqualTo("Visitor Corrected");
        assertThat(confirmed.contact().workEmail()).isEqualTo("visitor@example.com");

        ExtractionDto latest = rest.getForObject(
                "/api/v1/inquiries/" + created.id() + "/extractions/latest", ExtractionDto.class);
        assertThat(latest).isNotNull();
        assertThat(latest.fields()).isNotEmpty();
        assertThat(latest.fields())
                .allMatch(f -> !"PENDING".equals(f.reviewState()));
        assertThat(latest.fields())
                .anyMatch(f -> "full_name".equals(f.fieldKey()) && "CORRECTED".equals(f.reviewState()));
    }

    private FileAssetDto upload(UUID inquiryId, byte[] png) {
        ByteArrayResource resource = new ByteArrayResource(png) {
            @Override
            public String getFilename() {
                return "card-qr.png";
            }
        };
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.IMAGE_PNG);
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(resource, fileHeaders);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);
        ResponseEntity<FileAssetDto> response = rest.postForEntity(
                "/api/v1/inquiries/" + inquiryId + "/files?purpose=BUSINESS_CARD&side=front",
                new HttpEntity<>(body),
                FileAssetDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private static byte[] qrPng(String payload) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 240, 240);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "PNG", out);
        return out.toByteArray();
    }
}
