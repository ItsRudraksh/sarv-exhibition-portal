package com.sarv.exhibitionportal.consent;

import com.sarv.exhibitionportal.api.dto.ConsentDto;
import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.inquiry.InquiryRepository;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConsentService {

    static final Set<String> PURPOSES = Set.of(
            "LOCATION_EVIDENCE", "BUSINESS_CARD_EXTRACTION", "VOICE_PROCESSING");
    static final Set<String> DECISIONS = Set.of("GRANTED", "DECLINED", "REVOKED");

    private final ConsentRepository consents;
    private final InquiryRepository inquiries;
    private final AuditService audits;
    private final ExhibitionProperties properties;

    public ConsentService(
            ConsentRepository consents,
            InquiryRepository inquiries,
            AuditService audits,
            ExhibitionProperties properties
    ) {
        this.consents = consents;
        this.inquiries = inquiries;
        this.audits = audits;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<ConsentDto> list(UUID inquiryId) {
        requireInquiry(inquiryId);
        return consents.list(inquiryId);
    }

    @Transactional
    public ConsentDto record(UUID inquiryId, String purpose, String decision) {
        requireInquiry(inquiryId);
        if (purpose == null || !PURPOSES.contains(purpose)) {
            throw new InquiryValidationException("Unknown consent purpose.");
        }
        if ("LOCATION_EVIDENCE".equals(purpose) && "GRANTED".equals(decision)) {
            throw new InquiryValidationException("Location is not collected in this release.");
        }
        if (decision == null || !DECISIONS.contains(decision)) {
            throw new InquiryValidationException("Consent must be granted, declined, or revoked.");
        }
        String policy = properties.consentPolicyVersion() == null
                ? "card-extraction-v1"
                : properties.consentPolicyVersion();
        var latest = consents.latest(inquiryId, purpose);
        if (latest.isPresent() && latest.get().decision().equals(decision)
                && policy.equals(latest.get().policyVersion())) {
            return latest.get();
        }
        UUID id = UUID.randomUUID();
        Instant revokedAt = "REVOKED".equals(decision) ? Instant.now() : null;
        consents.insert(id, inquiryId, purpose, policy, decision, revokedAt);
        audits.record(inquiryId, "CONSENT", id, "CONSENT_RECORDED", Map.of(
                "purpose", purpose,
                "decision", decision
        ));
        return consents.latest(inquiryId, purpose).orElseThrow();
    }

    private void requireInquiry(UUID inquiryId) {
        if (!inquiries.exists(inquiryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry not found");
        }
    }
}
