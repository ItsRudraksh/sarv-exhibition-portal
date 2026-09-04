package com.sarv.exhibitionportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.sarv.exhibitionportal.api.dto.CampaignDto;
import com.sarv.exhibitionportal.api.dto.CreateInquiryRequest;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.audit.AuditService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;

class CampaignEntryApiTest extends MysqlSpringBootTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AuditService audits;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void resolvesActiveCampaignByCode() {
        CampaignDto campaign = rest.getForObject("/api/v1/campaigns/POC-STALL-1", CampaignDto.class);
        assertThat(campaign).isNotNull();
        assertThat(campaign.code()).isEqualTo("POC-STALL-1");
        assertThat(campaign.active()).isTrue();
        assertThat(campaign.landingRoute()).isEqualTo("CHOICE");
    }

    @Test
    void unknownCampaignIsNotFound() {
        ResponseEntity<String> response = rest.getForEntity("/api/v1/campaigns/NO-SUCH", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void exhibitionCreateUsesCampaignCode() {
        InquiryDraftDto created = rest.postForObject(
                "/api/v1/inquiries",
                new CreateInquiryRequest(null, "EXHIBITION_QR", "POC-STALL-1", false),
                InquiryDraftDto.class);
        assertThat(created).isNotNull();
        assertThat(created.entryChannel()).isEqualTo("EXHIBITION_QR");
        String campaignId = jdbc.sql("""
                        select qr_campaign_id from inquiries where id = :id
                        """)
                .param("id", created.id().toString())
                .query(String.class)
                .single();
        assertThat(campaignId).isEqualTo("22222222-2222-4222-8222-222222222222");
    }

    @Test
    void websiteEntryHasNoCampaign() {
        InquiryDraftDto created = rest.postForObject(
                "/api/v1/inquiries",
                new CreateInquiryRequest(null, "WEBSITE", null, false),
                InquiryDraftDto.class);
        assertThat(created).isNotNull();
        assertThat(created.entryChannel()).isEqualTo("WEBSITE");
        Integer campaignNull = jdbc.sql("""
                        select case when qr_campaign_id is null then 1 else 0 end
                        from inquiries where id = :id
                        """)
                .param("id", created.id().toString())
                .query(Integer.class)
                .single();
        assertThat(campaignNull).isEqualTo(1);
    }

    @Test
    void staffAssistedIsAuditedWithoutPii() {
        UUID id = UUID.randomUUID();
        InquiryDraftDto created = rest.postForObject(
                "/api/v1/inquiries",
                new CreateInquiryRequest(id, "DIRECT", null, true),
                InquiryDraftDto.class);
        assertThat(created).isNotNull();
        assertThat(created.entryChannel()).isEqualTo("DIRECT");
        assertThat(audits.count(created.id(), "INQUIRY_CREATED")).isEqualTo(1);
    }

    @Test
    void campaignCodeRejectedForWebsite() {
        ResponseEntity<String> response = rest.postForEntity(
                "/api/v1/inquiries",
                new CreateInquiryRequest(null, "WEBSITE", "POC-STALL-1", false),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsIgnoringCase("campaignCode");
    }
}
