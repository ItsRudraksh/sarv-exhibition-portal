package com.sarv.exhibitionportal.outbox;

import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.fileasset.LocalObjectStorage;
import com.sarv.exhibitionportal.inquiry.InquiryRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {

    public static final String MARKETING_LEAD = "MARKETING_LEAD";
    public static final String VENDOR_UPSERT = "VENDOR_UPSERT";

    private final OutboxRepository outbox;
    private final InquiryRepository inquiries;
    private final LocalObjectStorage storage;
    private final ExhibitionProperties properties;
    private final AuditService audits;

    public OutboxService(
            OutboxRepository outbox,
            InquiryRepository inquiries,
            LocalObjectStorage storage,
            ExhibitionProperties properties,
            AuditService audits
    ) {
        this.outbox = outbox;
        this.inquiries = inquiries;
        this.storage = storage;
        this.properties = properties;
        this.audits = audits;
    }

    @Transactional
    public void enqueueMarketingLead(UUID inquiryId) {
        ExhibitionProperties.Outbox cfg = properties.outbox();
        boolean created = outbox.insertIfAbsent(
                inquiryId,
                MARKETING_LEAD,
                cfg.marketingDestination(),
                MARKETING_LEAD + ":" + inquiryId
        );
        if (created) {
            outbox.markPurchaseQueued(inquiryId);
            inquiries.insertWorkflowEvent(inquiryId, "CRM_DELIVERY", "SUBMITTED", "QUEUED", "SYSTEM", null);
            audits.record(inquiryId, "INTEGRATION_DELIVERY", inquiryId, "OUTBOX_ENQUEUED", Map.of(
                    "kind", MARKETING_LEAD
            ));
        }
    }

    @Transactional
    public void enqueueVendorUpsert(UUID inquiryId) {
        ExhibitionProperties.Outbox cfg = properties.outbox();
        boolean created = outbox.insertIfAbsent(
                inquiryId,
                VENDOR_UPSERT,
                cfg.vendorDestination(),
                VENDOR_UPSERT + ":" + inquiryId
        );
        if (created) {
            inquiries.insertWorkflowEvent(inquiryId, "VENDOR_DELIVERY", "QUEUED", "PENDING", "SYSTEM", null);
            audits.record(inquiryId, "INTEGRATION_DELIVERY", inquiryId, "OUTBOX_ENQUEUED", Map.of(
                    "kind", VENDOR_UPSERT
            ));
        }
    }

    @Transactional
    public int processDue() {
        int processed = 0;
        for (OutboxRepository.DeliveryRow row : outbox.due(20)) {
            if (!outbox.claim(row.id())) {
                continue;
            }
            processed++;
            processClaimed(row);
        }
        return processed;
    }

    public long count(UUID inquiryId, String kind) {
        return outbox.count(inquiryId, kind);
    }

    public String latestState(UUID inquiryId, String kind) {
        return outbox.latestState(inquiryId, kind).orElse(null);
    }

    private void processClaimed(OutboxRepository.DeliveryRow row) {
        ExhibitionProperties.Outbox cfg = properties.outbox();
        if (VENDOR_UPSERT.equals(row.kind())) {
            outbox.markVendorInProgress(row.inquiryId());
        }
        try {
            String forced = cfg.forceFailureCode();
            if (forced != null && !forced.isBlank()) {
                throw new DeliveryFailedException(forced, "Forced stub failure");
            }
            String reference = outbox.referenceCode(row.inquiryId()).orElse("unknown");
            String body = """
                    {"inquiryId":"%s","referenceCode":"%s","kind":"%s","destination":"%s","payloadVersion":"%s"}
                    """.formatted(
                    row.inquiryId(),
                    reference,
                    row.kind(),
                    row.destination(),
                    row.payloadVersion()
            ).strip() + "\n";
            String key = "outbox/" + row.kind().toLowerCase() + "/" + row.id() + ".json";
            storage.write(Path.of(properties.storageRoot()), key, body.getBytes(StandardCharsets.UTF_8));
            outbox.markSucceeded(row.id(), key);
            if (MARKETING_LEAD.equals(row.kind())) {
                outbox.markPurchaseDispatched(row.inquiryId());
                inquiries.insertWorkflowEvent(row.inquiryId(), "CRM_DELIVERY", "QUEUED", "DISPATCHED", "SYSTEM", null);
            } else {
                outbox.markVendorSucceeded(row.inquiryId());
                inquiries.insertWorkflowEvent(row.inquiryId(), "VENDOR_DELIVERY", "IN_PROGRESS", "SUCCEEDED", "SYSTEM", null);
            }
            audits.record(row.inquiryId(), "INTEGRATION_DELIVERY", row.id(), "OUTBOX_SUCCEEDED", Map.of(
                    "kind", row.kind()
            ));
        } catch (Exception ex) {
            String code = ex instanceof DeliveryFailedException failed ? failed.code() : "STUB_FAILED";
            String message = sanitize(ex.getMessage());
            int attemptsAfter = row.attemptCount() + 1;
            if (attemptsAfter >= cfg.maxAttempts()) {
                outbox.markFailed(row.id(), code, message);
                if (MARKETING_LEAD.equals(row.kind())) {
                    outbox.markPurchaseDeliveryFailed(row.inquiryId());
                    inquiries.insertWorkflowEvent(
                            row.inquiryId(), "CRM_DELIVERY", "QUEUED", "DELIVERY_FAILED", "SYSTEM", null);
                } else {
                    outbox.markVendorFailed(row.inquiryId());
                    inquiries.insertWorkflowEvent(
                            row.inquiryId(), "VENDOR_DELIVERY", "IN_PROGRESS", "FAILED", "SYSTEM", null);
                }
                audits.record(row.inquiryId(), "INTEGRATION_DELIVERY", row.id(), "OUTBOX_FAILED", Map.of(
                        "kind", row.kind(),
                        "code", code
                ));
            } else {
                Instant next = Instant.now().plus(cfg.backoffSeconds() * (long) attemptsAfter, ChronoUnit.SECONDS);
                outbox.markRetry(row.id(), code, message, next);
                audits.record(row.inquiryId(), "INTEGRATION_DELIVERY", row.id(), "OUTBOX_RETRY_SCHEDULED", Map.of(
                        "kind", row.kind(),
                        "code", code
                ));
            }
        }
        if (!outbox.inquiryExists(row.inquiryId())) {
            throw new IllegalStateException("Outbox must never drop the source inquiry");
        }
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "delivery-failed";
        }
        String cleaned = raw.replaceAll("[\\w.%+-]+@[\\w.-]+", "[redacted]")
                .replaceAll("\\+?\\d[\\d\\s-]{7,}", "[redacted]");
        if (cleaned.length() > 180) {
            return cleaned.substring(0, 180);
        }
        return cleaned;
    }
}
