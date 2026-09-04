-- Phase 6: assistive OCR/QR field proposals.
-- AI may propose values; visitors review them. AI never approves vendors
-- and never silently overwrites confirmed contact fields.
-- Visitor review does not require app_users (reviewed_by_user_id stays null).

CREATE TABLE ai_assistance_sessions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    inquiry_id CHAR(36) NOT NULL,
    consent_id CHAR(36) NOT NULL,
    feature VARCHAR(32) NOT NULL,
    language_code VARCHAR(10),
    state VARCHAR(16) NOT NULL DEFAULT 'STARTED',
    provider_request_reference VARCHAR(255),
    started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT ai_sessions_feature_valid CHECK (feature IN ('VOICE_INPUT', 'BUSINESS_CARD_SCAN')),
    CONSTRAINT ai_sessions_state_valid CHECK (state IN (
        'STARTED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'
    )),
    CONSTRAINT ai_sessions_completed_state_valid CHECK (
        (state IN ('COMPLETED', 'FAILED', 'CANCELLED') AND completed_at IS NOT NULL)
        OR state NOT IN ('COMPLETED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT ai_sessions_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE RESTRICT,
    CONSTRAINT ai_sessions_consent_fk FOREIGN KEY (consent_id) REFERENCES consent_records(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_extractions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    input_asset_id CHAR(36),
    state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    provider_model_reference VARCHAR(255),
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT ai_extractions_state_valid CHECK (state IN (
        'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
    )),
    CONSTRAINT ai_extractions_completed_state_valid CHECK (
        (state IN ('COMPLETED', 'FAILED') AND completed_at IS NOT NULL)
        OR state NOT IN ('COMPLETED', 'FAILED')
    ),
    CONSTRAINT ai_extractions_session_fk FOREIGN KEY (session_id) REFERENCES ai_assistance_sessions(id) ON DELETE RESTRICT,
    CONSTRAINT ai_extractions_asset_fk FOREIGN KEY (input_asset_id) REFERENCES file_assets(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_extracted_fields (
    id CHAR(36) NOT NULL PRIMARY KEY,
    extraction_id CHAR(36) NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    proposed_value_text TEXT,
    confidence_score DECIMAL(4, 3),
    review_state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reviewed_by_user_id CHAR(36),
    reviewed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT ai_fields_confidence_valid CHECK (
        confidence_score IS NULL OR (confidence_score >= 0 AND confidence_score <= 1)
    ),
    CONSTRAINT ai_fields_review_state_valid CHECK (review_state IN (
        'PENDING', 'ACCEPTED', 'CORRECTED', 'REJECTED'
    )),
    CONSTRAINT ai_fields_review_valid CHECK (
        (review_state = 'PENDING' AND reviewed_at IS NULL)
        OR (review_state <> 'PENDING' AND reviewed_at IS NOT NULL)
    ),
    CONSTRAINT ai_fields_extraction_field_unique UNIQUE (extraction_id, field_key),
    CONSTRAINT ai_fields_extraction_fk FOREIGN KEY (extraction_id) REFERENCES ai_extractions(id) ON DELETE RESTRICT,
    CONSTRAINT ai_fields_reviewer_fk FOREIGN KEY (reviewed_by_user_id) REFERENCES app_users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ai_sessions_inquiry_started_idx
    ON ai_assistance_sessions (inquiry_id, started_at DESC);
CREATE INDEX ai_extractions_session_idx
    ON ai_extractions (session_id, created_at DESC);
CREATE INDEX ai_fields_extraction_idx
    ON ai_extracted_fields (extraction_id);
