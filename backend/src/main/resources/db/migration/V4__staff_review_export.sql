-- Phase 4: internal staff, supplier review, buyer queue, export jobs.
-- password_hash is a local-POC login column (SSO / external_subject later).
-- production_state stays NOT_REQUESTED until Add to production (APPROVE).
-- No integration_deliveries / vendor API in this migration.

SET search_path TO exhibition_portal, public;

CREATE TABLE app_users (
    id uuid PRIMARY KEY,
    external_subject text NOT NULL UNIQUE,
    email_normalized text NOT NULL UNIQUE,
    display_name text NOT NULL,
    password_hash text NOT NULL,
    status text NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    id uuid PRIMARY KEY,
    code varchar(64) NOT NULL UNIQUE,
    name text NOT NULL,
    description text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    assigned_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by_user_id uuid REFERENCES app_users(id) ON DELETE RESTRICT,
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (id, code, name, description)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'ADMIN', 'Administrator', 'Full administrative access.'),
    ('00000000-0000-4000-8000-000000000002', 'SUPPLIER_REVIEWER', 'Supplier reviewer', 'Reviews and approves supplier candidates.'),
    ('00000000-0000-4000-8000-000000000003', 'MARKETING', 'Marketing user', 'Works purchase leads and records follow-up.'),
    ('00000000-0000-4000-8000-000000000004', 'EXPORTER', 'Export user', 'Creates controlled lead exports.'),
    ('00000000-0000-4000-8000-000000000005', 'TAXONOMY_MANAGER', 'Taxonomy manager', 'Manages departments, product types, products, and standards.');

-- Local POC passwords are {noop}poc-staff. Do not use this scheme in production.
INSERT INTO app_users (id, external_subject, email_normalized, display_name, password_hash, status)
VALUES
    ('44444444-4444-4444-8444-444444444441', 'poc-reviewer', 'reviewer@sarv.local', 'POC Reviewer', '{noop}poc-staff', 'ACTIVE'),
    ('44444444-4444-4444-8444-444444444442', 'poc-marketing', 'marketing@sarv.local', 'POC Marketing', '{noop}poc-staff', 'ACTIVE'),
    ('44444444-4444-4444-8444-444444444443', 'poc-admin', 'admin@sarv.local', 'POC Admin', '{noop}poc-staff', 'ACTIVE');

INSERT INTO user_roles (user_id, role_id)
VALUES
    ('44444444-4444-4444-8444-444444444441', '00000000-0000-4000-8000-000000000002'),
    ('44444444-4444-4444-8444-444444444442', '00000000-0000-4000-8000-000000000003'),
    ('44444444-4444-4444-8444-444444444442', '00000000-0000-4000-8000-000000000004'),
    ('44444444-4444-4444-8444-444444444443', '00000000-0000-4000-8000-000000000001');

ALTER TABLE supplier_inquiries
    ADD COLUMN assigned_to_user_id uuid REFERENCES app_users(id) ON DELETE RESTRICT,
    ADD COLUMN approved_at timestamptz,
    ADD COLUMN approved_by_user_id uuid REFERENCES app_users(id) ON DELETE RESTRICT;

ALTER TABLE supplier_inquiries
    ADD CONSTRAINT supplier_inquiries_approval_valid CHECK (
        (review_state = 'APPROVED' AND approved_at IS NOT NULL AND approved_by_user_id IS NOT NULL)
        OR (
            review_state <> 'APPROVED'
            AND approved_at IS NULL
            AND approved_by_user_id IS NULL
        )
    );

ALTER TABLE purchase_inquiries
    ADD COLUMN assigned_to_user_id uuid REFERENCES app_users(id) ON DELETE RESTRICT,
    ADD COLUMN first_dispatched_at timestamptz,
    ADD COLUMN marketing_notes text;

CREATE TABLE review_cases (
    id uuid PRIMARY KEY,
    supplier_inquiry_id uuid NOT NULL REFERENCES supplier_inquiries(inquiry_id) ON DELETE RESTRICT,
    state text NOT NULL DEFAULT 'OPEN'
        CHECK (state IN ('OPEN', 'IN_REVIEW', 'WAITING_FOR_INFO', 'CLOSED')),
    assigned_to_user_id uuid REFERENCES app_users(id) ON DELETE RESTRICT,
    opened_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT review_cases_closed_state_valid CHECK (
        (state = 'CLOSED' AND closed_at IS NOT NULL)
        OR (state <> 'CLOSED' AND closed_at IS NULL)
    )
);

CREATE UNIQUE INDEX review_cases_one_open_idx
    ON review_cases (supplier_inquiry_id)
    WHERE state <> 'CLOSED';

CREATE TABLE review_decisions (
    id uuid PRIMARY KEY,
    review_case_id uuid NOT NULL REFERENCES review_cases(id) ON DELETE RESTRICT,
    decision text NOT NULL CHECK (decision IN (
        'REQUEST_INFORMATION',
        'APPROVE',
        'REJECT'
    )),
    reason_code text,
    notes text,
    decided_by_user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    decided_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Export bytes live on disk (storage_key). Not a raw table dump. Not tied to one inquiry file_asset.
CREATE TABLE export_jobs (
    id uuid PRIMARY KEY,
    requested_by_user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    scope text NOT NULL CHECK (scope IN ('PURCHASE_LEADS', 'SUPPLIER_INQUIRIES')),
    filter_summary jsonb NOT NULL DEFAULT '{}'::jsonb
        CHECK (jsonb_typeof(filter_summary) = 'object'),
    state text NOT NULL DEFAULT 'QUEUED'
        CHECK (state IN ('QUEUED', 'GENERATING', 'READY', 'FAILED', 'EXPIRED')),
    storage_key text,
    original_filename text,
    media_type text,
    byte_size bigint,
    expires_at timestamptz NOT NULL,
    generated_at timestamptz,
    failure_reason text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT export_jobs_ready_state_valid CHECK (
        (state = 'READY' AND storage_key IS NOT NULL AND generated_at IS NOT NULL)
        OR state <> 'READY'
    )
);

ALTER TABLE workflow_events
    ADD COLUMN actor_user_id uuid REFERENCES app_users(id) ON DELETE RESTRICT;

ALTER TABLE audit_events
    ADD CONSTRAINT audit_events_actor_user_fk
    FOREIGN KEY (actor_user_id) REFERENCES app_users(id) ON DELETE RESTRICT;

CREATE INDEX supplier_inquiries_review_state_idx
    ON supplier_inquiries (review_state, updated_at DESC);
CREATE INDEX purchase_inquiries_lead_state_idx
    ON purchase_inquiries (lead_state, updated_at DESC);
CREATE INDEX review_decisions_case_idx
    ON review_decisions (review_case_id, decided_at DESC);
CREATE INDEX export_jobs_requester_idx
    ON export_jobs (requested_by_user_id, created_at DESC);
