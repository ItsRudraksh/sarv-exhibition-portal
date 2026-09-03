package com.sarv.exhibitionportal.outbox;

import com.sarv.exhibitionportal.config.JdbcUuids;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class OutboxRepository {

    private final JdbcClient jdbc;

    public OutboxRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean insertIfAbsent(
            UUID inquiryId,
            String kind,
            String destination,
            String idempotencyKey
    ) {
        int updated = jdbc.sql("""
                insert ignore into integration_deliveries (
                    id, inquiry_id, delivery_kind, destination, idempotency_key,
                    state, next_attempt_at, request_payload_version
                ) values (
                    :id, :inquiry, :kind, :dest, :key,
                    'PENDING', CURRENT_TIMESTAMP, 'v1'
                )
                """)
                .param("id", JdbcUuids.mysql(UUID.randomUUID()))
                .param("inquiry", JdbcUuids.mysql(inquiryId))
                .param("kind", JdbcUuids.mysql(kind))
                .param("dest", JdbcUuids.mysql(destination))
                .param("key", JdbcUuids.mysql(idempotencyKey))
                .update();
        return updated > 0;
    }

    public long count(UUID inquiryId, String kind) {
        Long count = jdbc.sql("""
                select count(*) from integration_deliveries
                where inquiry_id = :id and delivery_kind = :kind
                """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .param("kind", JdbcUuids.mysql(kind))
                .query(Long.class)
                .single();
        return count == null ? 0 : count;
    }

    public Optional<String> latestState(UUID inquiryId, String kind) {
        return jdbc.sql("""
                select state from integration_deliveries
                where inquiry_id = :id and delivery_kind = :kind
                order by created_at desc
                limit 1
                """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .param("kind", JdbcUuids.mysql(kind))
                .query(String.class)
                .optional();
    }

    public List<DeliveryRow> due(int limit) {
        return jdbc.sql("""
                select id, inquiry_id, delivery_kind, destination, idempotency_key, state,
                       attempt_count, request_payload_version
                from integration_deliveries
                where state in ('PENDING', 'RETRY_SCHEDULED')
                  and (next_attempt_at is null or next_attempt_at <= CURRENT_TIMESTAMP)
                order by created_at
                limit :limit
                """)
                .param("limit", JdbcUuids.mysql(limit))
                .query((rs, n) -> new DeliveryRow(
                        JdbcUuids.get(rs, "id"),
                        JdbcUuids.get(rs, "inquiry_id"),
                        rs.getString("delivery_kind"),
                        rs.getString("destination"),
                        rs.getString("idempotency_key"),
                        rs.getString("state"),
                        rs.getInt("attempt_count"),
                        rs.getString("request_payload_version")
                ))
                .list();
    }

    public boolean claim(UUID id) {
        int updated = jdbc.sql("""
                update integration_deliveries
                set state = 'IN_PROGRESS',
                    last_attempt_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                where id = :id and state in ('PENDING', 'RETRY_SCHEDULED')
                """)
                .param("id", JdbcUuids.mysql(id))
                .update();
        return updated == 1;
    }

    public void markSucceeded(UUID id, String externalReference) {
        jdbc.sql("""
                 update integration_deliveries
                 set state = 'SUCCEEDED',
                     attempt_count = attempt_count + 1,
                     last_attempt_at = CURRENT_TIMESTAMP,
                     next_attempt_at = null,
                     external_reference = :ref,
                     last_error_code = null,
                     last_error_message = null,
                     updated_at = CURRENT_TIMESTAMP
                 where id = :id
                 """)
                .param("id", JdbcUuids.mysql(id))
                .param("ref", JdbcUuids.mysql(externalReference))
                .update();
    }

    public void markRetry(UUID id, String code, String message, Instant nextAttempt) {
        jdbc.sql("""
                 update integration_deliveries
                 set state = 'RETRY_SCHEDULED',
                     attempt_count = attempt_count + 1,
                     last_attempt_at = CURRENT_TIMESTAMP,
                     next_attempt_at = :next,
                     last_error_code = :code,
                     last_error_message = :message,
                     updated_at = CURRENT_TIMESTAMP
                 where id = :id
                 """)
                .param("id", JdbcUuids.mysql(id))
                .param("next", JdbcUuids.mysql(Timestamp.from(nextAttempt)))
                .param("code", JdbcUuids.mysql(code))
                .param("message", JdbcUuids.mysql(message))
                .update();
    }

    public void markFailed(UUID id, String code, String message) {
        jdbc.sql("""
                 update integration_deliveries
                 set state = 'FAILED',
                     attempt_count = attempt_count + 1,
                     last_attempt_at = CURRENT_TIMESTAMP,
                     next_attempt_at = null,
                     last_error_code = :code,
                     last_error_message = :message,
                     updated_at = CURRENT_TIMESTAMP
                 where id = :id
                 """)
                .param("id", JdbcUuids.mysql(id))
                .param("code", JdbcUuids.mysql(code))
                .param("message", JdbcUuids.mysql(message))
                .update();
    }

    public Optional<String> referenceCode(UUID inquiryId) {
        return jdbc.sql("select reference_code from inquiries where id = :id")
                .param("id", JdbcUuids.mysql(inquiryId))
                .query(String.class)
                .optional();
    }

    public boolean inquiryExists(UUID inquiryId) {
        Long count = jdbc.sql("select count(*) from inquiries where id = :id")
                .param("id", JdbcUuids.mysql(inquiryId))
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    public void markPurchaseQueued(UUID inquiryId) {
        jdbc.sql("""
                 update purchase_inquiries
                 set lead_state = 'QUEUED', updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id and lead_state in ('SUBMITTED', 'QUEUED')
                 """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
    }

    public void markPurchaseDispatched(UUID inquiryId) {
        jdbc.sql("""
                 update purchase_inquiries
                 set lead_state = 'DISPATCHED',
                     first_dispatched_at = coalesce(first_dispatched_at, CURRENT_TIMESTAMP),
                     updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
    }

    public void markPurchaseDeliveryFailed(UUID inquiryId) {
        jdbc.sql("""
                 update purchase_inquiries
                 set lead_state = 'DELIVERY_FAILED', updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
    }

    public void markVendorInProgress(UUID inquiryId) {
        jdbc.sql("""
                 update supplier_inquiries
                 set production_state = 'IN_PROGRESS', updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id and production_state in ('QUEUED', 'IN_PROGRESS', 'FAILED')
                 """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
    }

    public void markVendorSucceeded(UUID inquiryId) {
        jdbc.sql("""
                 update supplier_inquiries
                 set production_state = 'SUCCEEDED', updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
    }

    public void markVendorFailed(UUID inquiryId) {
        jdbc.sql("""
                 update supplier_inquiries
                 set production_state = 'FAILED', updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
    }

    public record DeliveryRow(
            UUID id,
            UUID inquiryId,
            String kind,
            String destination,
            String idempotencyKey,
            String state,
            int attemptCount,
            String payloadVersion
    ) {}
}
