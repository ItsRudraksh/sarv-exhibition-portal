package com.sarv.exhibitionportal;

import com.sarv.exhibitionportal.api.dto.ConsentDto;
import com.sarv.exhibitionportal.api.dto.FileAssetDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.audit.AuditService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(
        type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class FileConsentAuditApiTest {

    private static final byte[] JPEG = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x10, 0x11, 0x12};

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AuditService audits;

    @Test
    void createWritesAuditEvent() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", null, InquiryDraftDto.class);
        assertThat(created).isNotNull();
        assertThat(audits.count(created.id(), "INQUIRY_CREATED")).isEqualTo(1);
    }

    @Test
    void cardUploadStoresMetadataAndGrantsConsent() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", null, InquiryDraftDto.class);
        assertThat(created).isNotNull();
        FileAssetDto asset = upload(created.id(), "BUSINESS_CARD", "front", "card.jpg", "image/jpeg", JPEG);
        assertThat(asset.securityScanState()).isEqualTo("CLEAN");
        assertThat(asset.processingState()).isEqualTo("READY");
        InquiryDraftDto reloaded = rest.getForObject("/api/v1/inquiries/" + created.id(), InquiryDraftDto.class);
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.cardFront()).isNotNull();
        assertThat(reloaded.cardFront().assetId()).isEqualTo(asset.id());
        ResponseEntity<ConsentDto[]> consents = rest.getForEntity(
                "/api/v1/inquiries/" + created.id() + "/consents", ConsentDto[].class);
        assertThat(consents.getBody()).isNotEmpty();
        assertThat(consents.getBody()[0].purpose()).isEqualTo("BUSINESS_CARD_EXTRACTION");
        assertThat(consents.getBody()[0].decision()).isEqualTo("GRANTED");
        ResponseEntity<byte[]> bytes = rest.getForEntity(
                "/api/v1/inquiries/" + created.id() + "/files/" + asset.id(), byte[].class);
        assertThat(bytes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bytes.getBody()).startsWith((byte) 0xFF);
        assertThat(audits.count(created.id(), "FILE_UPLOADED")).isEqualTo(1);
    }

    @Test
    void rejectedScanKeepsOriginalAndDoesNotServeBytes() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", null, InquiryDraftDto.class);
        assertThat(created).isNotNull();
        byte[] fakeJpeg = new byte[] {'M', 'Z', 0x00, 0x01, 0x02, 0x03};
        ResponseEntity<String> response = uploadRaw(
                created.id(), "BUSINESS_CARD", "front", "card.jpg", "image/jpeg", fakeJpeg);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("contents");
        assertThat(audits.count(created.id(), "FILE_SCAN_REJECTED")).isEqualTo(1);
        InquiryDraftDto reloaded = rest.getForObject("/api/v1/inquiries/" + created.id(), InquiryDraftDto.class);
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.cardFront()).isNull();
    }

    @Test
    void decliningCardConsentIsAppendOnly() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", null, InquiryDraftDto.class);
        assertThat(created).isNotNull();
        ConsentDto declined = rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/consents",
                java.util.Map.of("purpose", "BUSINESS_CARD_EXTRACTION", "decision", "DECLINED"),
                ConsentDto.class);
        assertThat(declined.decision()).isEqualTo("DECLINED");
        rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/consents",
                java.util.Map.of("purpose", "BUSINESS_CARD_EXTRACTION", "decision", "GRANTED"),
                ConsentDto.class);
        ResponseEntity<ConsentDto[]> all = rest.getForEntity(
                "/api/v1/inquiries/" + created.id() + "/consents", ConsentDto[].class);
        assertThat(all.getBody()).hasSize(2);
        assertThat(all.getBody()[0].decision()).isEqualTo("GRANTED");
        assertThat(audits.count(created.id(), "CONSENT_RECORDED")).isEqualTo(2);
    }

    @Test
    void locationGrantIsRejected() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", null, InquiryDraftDto.class);
        assertThat(created).isNotNull();
        ResponseEntity<String> response = rest.postForEntity(
                "/api/v1/inquiries/" + created.id() + "/consents",
                java.util.Map.of("purpose", "LOCATION_EVIDENCE", "decision", "GRANTED"),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private FileAssetDto upload(
            UUID inquiryId, String purpose, String side, String filename, String type, byte[] bytes
    ) {
        ResponseEntity<FileAssetDto> response = rest.postForEntity(
                "/api/v1/inquiries/" + inquiryId + "/files?purpose=" + purpose + "&side=" + side,
                fileEntity(filename, type, bytes),
                FileAssetDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private ResponseEntity<String> uploadRaw(
            UUID inquiryId, String purpose, String side, String filename, String type, byte[] bytes
    ) {
        return rest.postForEntity(
                "/api/v1/inquiries/" + inquiryId + "/files?purpose=" + purpose + "&side=" + side,
                fileEntity(filename, type, bytes),
                String.class);
    }

    private static HttpEntity<MultiValueMap<String, Object>> fileEntity(
            String filename, String type, byte[] bytes
    ) {
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(type));
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(resource, fileHeaders);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);
        return new HttpEntity<>(body);
    }
}
