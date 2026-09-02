-- Phase 5: durable outbox. No live CRM or vendor API in this POC.
-- MARKETING_LEAD is enqueued on purchase submit.
-- VENDOR_UPSERT is enqueued only after Add to production, never on visitor supplier submit.

SET search_path TO exhibition_portal, public;

CREATE TABLE integration_deliveries (
    id uuid PRIMARY KEY,
    inquiry_id uuid NOT NULL REFERENCES inquiries(id) ON DELETE RESTRICT,
    delivery_kind text NOT NULL CHECK (delivery_kind IN ('VENDOR_UPSERT', 'MARKETING_LEAD')),
    destination text NOT NULL,
    idempotency_key text NOT NULL UNIQUE,
    state text NOT NULL DEFAULT 'PENDING'
        CHECK (state IN (
            'PENDING',
            'IN_PROGRESS',
            'SUCCEEDED',
            'RETRY_SCHEDULED',
            'FAILED',
            'CANCELLED'
        )),
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at timestamptz,
    last_attempt_at timestamptz,
    external_reference text,
    last_error_code text,
    last_error_message text,
    request_payload_version varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX integration_deliveries_retry_idx
    ON integration_deliveries (state, next_attempt_at)
    WHERE state IN ('PENDING', 'RETRY_SCHEDULED');
CREATE INDEX integration_deliveries_inquiry_idx
    ON integration_deliveries (inquiry_id, created_at DESC);
