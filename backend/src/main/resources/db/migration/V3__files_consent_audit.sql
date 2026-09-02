-- Phase 3: private file metadata, consent events, audit trail.
-- Bytes are never stored in PostgreSQL.
-- Consent is append-only: current decision is the latest row per (inquiry_id, purpose).
-- Location evidence is not created here — do not capture GPS or raw IP.

SET search_path TO exhibition_portal, public;

CREATE TABLE catalogue_bundles (
    id uuid PRIMARY KEY,
    supplier_inquiry_id uuid NOT NULL REFERENCES supplier_inquiries(inquiry_id) ON DELETE RESTRICT,
    submission_format text NOT NULL CHECK (submission_format IN ('PDF', 'IMAGES', 'MIXED')),
    processing_state text NOT NULL DEFAULT 'PENDING_SCAN'
        CHECK (processing_state IN (
            'PENDING_SCAN', 'PROCESSING', 'READY', 'FAILED', 'REJECTED'
        )),
    failure_reason text,
    submitted_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT catalogue_bundles_completed_state_valid CHECK (
        (processing_state IN ('READY', 'FAILED', 'REJECTED') AND completed_at IS NOT NULL)
        OR processing_state NOT IN ('READY', 'FAILED', 'REJECTED')
    )
);

CREATE TABLE file_assets (
    id uuid PRIMARY KEY,
    inquiry_id uuid NOT NULL REFERENCES inquiries(id) ON DELETE RESTRICT,
    catalogue_bundle_id uuid REFERENCES catalogue_bundles(id) ON DELETE RESTRICT,
    source_asset_id uuid REFERENCES file_assets(id) ON DELETE RESTRICT,
    purpose text NOT NULL CHECK (purpose IN (
        'CATALOGUE_ORIGINAL',
        'CATALOGUE_DERIVED_PDF',
        'BUSINESS_CARD',
        'VOICE_INPUT',
        'EXCEL_EXPORT'
    )),
    original_filename text NOT NULL,
    media_type text NOT NULL,
    byte_size bigint NOT NULL CHECK (byte_size > 0),
    sha256_digest char(64) NOT NULL CHECK (sha256_digest ~ '^[0-9a-f]{64}$'),
    storage_key text NOT NULL UNIQUE,
    security_scan_state text NOT NULL DEFAULT 'PENDING'
        CHECK (security_scan_state IN ('PENDING', 'CLEAN', 'REJECTED', 'FAILED')),
    processing_state text NOT NULL DEFAULT 'UPLOADED'
        CHECK (processing_state IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED', 'PURGED')),
    retention_until timestamptz NOT NULL,
    purged_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT file_assets_catalogue_bundle_required CHECK (
        purpose NOT IN ('CATALOGUE_ORIGINAL', 'CATALOGUE_DERIVED_PDF')
        OR catalogue_bundle_id IS NOT NULL
    ),
    CONSTRAINT file_assets_derived_source_required CHECK (
        purpose <> 'CATALOGUE_DERIVED_PDF' OR source_asset_id IS NOT NULL
    )
);

CREATE TABLE consent_records (
    id uuid PRIMARY KEY,
    inquiry_id uuid NOT NULL REFERENCES inquiries(id) ON DELETE RESTRICT,
    purpose text NOT NULL CHECK (purpose IN (
        'LOCATION_EVIDENCE',
        'BUSINESS_CARD_EXTRACTION',
        'VOICE_PROCESSING'
    )),
    policy_version text NOT NULL,
    decision text NOT NULL CHECK (decision IN ('GRANTED', 'DECLINED', 'REVOKED')),
    decided_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT consent_records_revocation_valid CHECK (
        (decision = 'REVOKED' AND revoked_at IS NOT NULL)
        OR (decision <> 'REVOKED' AND revoked_at IS NULL)
    )
);

CREATE TABLE audit_events (
    id uuid PRIMARY KEY,
    inquiry_id uuid REFERENCES inquiries(id) ON DELETE RESTRICT,
    entity_type text NOT NULL,
    entity_id uuid NOT NULL,
    event_type text NOT NULL,
    actor_kind text NOT NULL CHECK (actor_kind IN ('VISITOR', 'USER', 'SYSTEM', 'INTEGRATION')),
    actor_user_id uuid,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK (jsonb_typeof(metadata) = 'object'),
    CONSTRAINT audit_events_actor_valid CHECK (
        (actor_kind = 'USER' AND actor_user_id IS NOT NULL)
        OR (actor_kind <> 'USER' AND actor_user_id IS NULL)
    )
);

ALTER TABLE inquiry_ui_state
    ADD COLUMN card_front_asset_id uuid REFERENCES file_assets(id) ON DELETE SET NULL,
    ADD COLUMN card_back_asset_id uuid REFERENCES file_assets(id) ON DELETE SET NULL;

ALTER TABLE supplier_inquiries
    ADD COLUMN catalogue_asset_id uuid REFERENCES file_assets(id) ON DELETE SET NULL,
    ADD COLUMN catalogue_bundle_id uuid REFERENCES catalogue_bundles(id) ON DELETE SET NULL;

CREATE INDEX file_assets_inquiry_purpose_state_idx
    ON file_assets (inquiry_id, purpose, processing_state);
CREATE INDEX file_assets_bundle_idx
    ON file_assets (catalogue_bundle_id)
    WHERE catalogue_bundle_id IS NOT NULL;
CREATE INDEX catalogue_bundles_processing_idx
    ON catalogue_bundles (processing_state, submitted_at);
CREATE INDEX consent_records_inquiry_purpose_idx
    ON consent_records (inquiry_id, purpose, decided_at DESC);
CREATE INDEX audit_events_inquiry_occurred_idx
    ON audit_events (inquiry_id, occurred_at DESC);
CREATE INDEX audit_events_entity_idx
    ON audit_events (entity_type, entity_id, occurred_at DESC);
