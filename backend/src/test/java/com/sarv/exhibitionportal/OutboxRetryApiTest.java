package com.sarv.exhibitionportal;

import com.sarv.exhibitionportal.api.dto.BuyerDto;
import com.sarv.exhibitionportal.api.dto.BuyerSpecificationsDto;
import com.sarv.exhibitionportal.api.dto.ContactDto;
import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.api.dto.SupplierDto;
import com.sarv.exhibitionportal.outbox.OutboxService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(
        type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@TestPropertySource(properties = {
        "exhibition.outbox.force-failure-code=STUB_REJECTED",
        "exhibition.outbox.max-attempts=1",
        "exhibition.outbox.backoff-seconds=0"
})
class OutboxRetryApiTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private OutboxService outbox;

    @Test
    void failedDeliveryKeepsSourceInquiry() {
        InquiryDraftDto created = rest.postForObject("/api/v1/inquiries", new InquiryDraftDto(
                null, "DRAFT", "card-capture", null, "EXHIBITION_QR",
                null, null, null,
                new ContactDto("", "", "+91", ""),
                new SupplierDto("", "", "", "", null),
                List.of(), List.of(),
                new BuyerDto("", "", new BuyerSpecificationsDto("", "", "", "", "")),
                false, null, null), InquiryDraftDto.class);
        assertThat(created).isNotNull();
        rest.postForObject(
                "/api/v1/inquiries/" + created.id() + "/submit",
                new InquiryDraftDto(
                        created.id(), "DRAFT", "buyer-review", "PURCHASE", "EXHIBITION_QR",
                        null, null, null,
                        new ContactDto("Asha Rao", "asha@example.com", "+91", "9876543210"),
                        new SupplierDto("", "", "", "", null),
                        List.of(), List.of(),
                        new BuyerDto("Need", "", new BuyerSpecificationsDto("", "", "", "", "")),
                        true, null, created.referenceCode()),
                InquiryDraftDto.class);
        outbox.processDue();
        assertThat(outbox.latestState(created.id(), OutboxService.MARKETING_LEAD)).isEqualTo("FAILED");
        InquiryDraftDto source = rest.getForObject("/api/v1/inquiries/" + created.id(), InquiryDraftDto.class);
        assertThat(source).isNotNull();
        assertThat(source.lifecycleState()).isEqualTo("SUBMITTED");
    }
}
