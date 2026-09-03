-- Phase 5: durable outbox. No live CRM or vendor API in this POC.
-- MARKETING_LEAD is enqueued on purchase submit.
-- VENDOR_UPSERT is enqueued only after Add to production, never on visitor supplier submit.

CREATE TABLE integration_deliveries (
    id CHAR(36) NOT NULL PRIMARY KEY,
    inquiry_id CHAR(36) NOT NULL,
    delivery_kind VARCHAR(32) NOT NULL,
    destination VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(191) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6),
    last_attempt_at DATETIME(6),
    external_reference VARCHAR(255),
    last_error_code VARCHAR(64),
    last_error_message TEXT,
    request_payload_version VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT integration_deliveries_kind_valid CHECK (delivery_kind IN ('VENDOR_UPSERT', 'MARKETING_LEAD')),
    CONSTRAINT integration_deliveries_state_valid CHECK (state IN (
        'PENDING',
        'IN_PROGRESS',
        'SUCCEEDED',
        'RETRY_SCHEDULED',
        'FAILED',
        'CANCELLED'
    )),
    CONSTRAINT integration_deliveries_attempts_valid CHECK (attempt_count >= 0),
    CONSTRAINT integration_deliveries_idempotency UNIQUE (idempotency_key),
    CONSTRAINT integration_deliveries_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX integration_deliveries_retry_idx
    ON integration_deliveries (state, next_attempt_at);
CREATE INDEX integration_deliveries_inquiry_idx
    ON integration_deliveries (inquiry_id, created_at);
