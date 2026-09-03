-- Phase 3: private file metadata, consent events, audit trail.
-- Bytes are never stored in MySQL.
-- Consent is append-only: current decision is the latest row per (inquiry_id, purpose).
-- Location evidence is not created here — do not capture GPS or raw IP.

CREATE TABLE catalogue_bundles (
    id CHAR(36) NOT NULL PRIMARY KEY,
    supplier_inquiry_id CHAR(36) NOT NULL,
    submission_format VARCHAR(16) NOT NULL,
    processing_state VARCHAR(32) NOT NULL DEFAULT 'PENDING_SCAN',
    failure_reason TEXT,
    submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT catalogue_bundles_format_valid CHECK (submission_format IN ('PDF', 'IMAGES', 'MIXED')),
    CONSTRAINT catalogue_bundles_state_valid CHECK (processing_state IN (
        'PENDING_SCAN', 'PROCESSING', 'READY', 'FAILED', 'REJECTED'
    )),
    CONSTRAINT catalogue_bundles_completed_state_valid CHECK (
        (processing_state IN ('READY', 'FAILED', 'REJECTED') AND completed_at IS NOT NULL)
        OR processing_state NOT IN ('READY', 'FAILED', 'REJECTED')
    ),
    CONSTRAINT catalogue_bundles_supplier_fk FOREIGN KEY (supplier_inquiry_id) REFERENCES supplier_inquiries(inquiry_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE file_assets (
    id CHAR(36) NOT NULL PRIMARY KEY,
    inquiry_id CHAR(36) NOT NULL,
    catalogue_bundle_id CHAR(36),
    source_asset_id CHAR(36),
    purpose VARCHAR(32) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    media_type VARCHAR(128) NOT NULL,
    byte_size BIGINT NOT NULL,
    sha256_digest CHAR(64) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    security_scan_state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    processing_state VARCHAR(16) NOT NULL DEFAULT 'UPLOADED',
    retention_until DATETIME(6) NOT NULL,
    purged_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT file_assets_byte_size_valid CHECK (byte_size > 0),
    CONSTRAINT file_assets_sha256_valid CHECK (sha256_digest REGEXP '^[0-9a-f]{64}$'),
    CONSTRAINT file_assets_storage_unique UNIQUE (storage_key),
    CONSTRAINT file_assets_purpose_valid CHECK (purpose IN (
        'CATALOGUE_ORIGINAL',
        'CATALOGUE_DERIVED_PDF',
        'BUSINESS_CARD',
        'VOICE_INPUT',
        'EXCEL_EXPORT'
    )),
    CONSTRAINT file_assets_scan_valid CHECK (security_scan_state IN ('PENDING', 'CLEAN', 'REJECTED', 'FAILED')),
    CONSTRAINT file_assets_proc_valid CHECK (processing_state IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED', 'PURGED')),
    CONSTRAINT file_assets_catalogue_bundle_required CHECK (
        purpose NOT IN ('CATALOGUE_ORIGINAL', 'CATALOGUE_DERIVED_PDF')
        OR catalogue_bundle_id IS NOT NULL
    ),
    CONSTRAINT file_assets_derived_source_required CHECK (
        purpose <> 'CATALOGUE_DERIVED_PDF' OR source_asset_id IS NOT NULL
    ),
    CONSTRAINT file_assets_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE RESTRICT,
    CONSTRAINT file_assets_bundle_fk FOREIGN KEY (catalogue_bundle_id) REFERENCES catalogue_bundles(id) ON DELETE RESTRICT,
    CONSTRAINT file_assets_source_fk FOREIGN KEY (source_asset_id) REFERENCES file_assets(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE consent_records (
    id CHAR(36) NOT NULL PRIMARY KEY,
    inquiry_id CHAR(36) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    policy_version VARCHAR(64) NOT NULL,
    decision VARCHAR(16) NOT NULL,
    decided_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    revoked_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT consent_records_purpose_valid CHECK (purpose IN (
        'LOCATION_EVIDENCE',
        'BUSINESS_CARD_EXTRACTION',
        'VOICE_PROCESSING'
    )),
    CONSTRAINT consent_records_decision_valid CHECK (decision IN ('GRANTED', 'DECLINED', 'REVOKED')),
    CONSTRAINT consent_records_revocation_valid CHECK (
        (decision = 'REVOKED' AND revoked_at IS NOT NULL)
        OR (decision <> 'REVOKED' AND revoked_at IS NULL)
    ),
    CONSTRAINT consent_records_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_events (
    id CHAR(36) NOT NULL PRIMARY KEY,
    inquiry_id CHAR(36),
    entity_type VARCHAR(64) NOT NULL,
    entity_id CHAR(36) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_kind VARCHAR(32) NOT NULL,
    actor_user_id CHAR(36),
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    metadata JSON NOT NULL,
    CONSTRAINT audit_events_actor_kind_valid CHECK (actor_kind IN ('VISITOR', 'USER', 'SYSTEM', 'INTEGRATION')),
    CONSTRAINT audit_events_actor_valid CHECK (
        (actor_kind = 'USER' AND actor_user_id IS NOT NULL)
        OR (actor_kind <> 'USER' AND actor_user_id IS NULL)
    ),
    CONSTRAINT audit_events_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE inquiry_ui_state
    ADD COLUMN card_front_asset_id CHAR(36) NULL,
    ADD COLUMN card_back_asset_id CHAR(36) NULL,
    ADD CONSTRAINT inquiry_ui_front_asset_fk FOREIGN KEY (card_front_asset_id) REFERENCES file_assets(id) ON DELETE SET NULL,
    ADD CONSTRAINT inquiry_ui_back_asset_fk FOREIGN KEY (card_back_asset_id) REFERENCES file_assets(id) ON DELETE SET NULL;

ALTER TABLE supplier_inquiries
    ADD COLUMN catalogue_asset_id CHAR(36) NULL,
    ADD COLUMN catalogue_bundle_id CHAR(36) NULL,
    ADD CONSTRAINT supplier_catalogue_asset_fk FOREIGN KEY (catalogue_asset_id) REFERENCES file_assets(id) ON DELETE SET NULL,
    ADD CONSTRAINT supplier_catalogue_bundle_fk FOREIGN KEY (catalogue_bundle_id) REFERENCES catalogue_bundles(id) ON DELETE SET NULL;

CREATE INDEX file_assets_inquiry_purpose_state_idx
    ON file_assets (inquiry_id, purpose, processing_state);
CREATE INDEX file_assets_bundle_idx
    ON file_assets (catalogue_bundle_id);
CREATE INDEX catalogue_bundles_processing_idx
    ON catalogue_bundles (processing_state, submitted_at);
CREATE INDEX consent_records_inquiry_purpose_idx
    ON consent_records (inquiry_id, purpose, decided_at);
CREATE INDEX audit_events_inquiry_occurred_idx
    ON audit_events (inquiry_id, occurred_at);
CREATE INDEX audit_events_entity_idx
    ON audit_events (entity_type, entity_id, occurred_at);
