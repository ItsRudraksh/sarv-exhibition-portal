package com.sarv.exhibitionportal.audit;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditRepository audits;

    public AuditService(AuditRepository audits) {
        this.audits = audits;
    }

    public void record(
            UUID inquiryId,
            String entityType,
            UUID entityId,
            String eventType,
            Map<String, Object> metadata
    ) {
        audits.insert(inquiryId, entityType, entityId, eventType, "VISITOR", null, metadata);
    }

    public void recordUser(
            UUID inquiryId,
            String entityType,
            UUID entityId,
            String eventType,
            UUID actorUserId,
            Map<String, Object> metadata
    ) {
        audits.insert(inquiryId, entityType, entityId, eventType, "USER", actorUserId, metadata);
    }

    public long count(UUID inquiryId, String eventType) {
        return audits.countByInquiryAndEvent(inquiryId, eventType);
    }
}
