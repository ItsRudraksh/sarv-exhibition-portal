-- Phase 4: internal staff, supplier review, buyer queue, export jobs.
-- password_hash is a local-POC login column (SSO / external_subject later).
-- production_state stays NOT_REQUESTED until Add to production (APPROVE).
-- open_supplier_key is application-maintained (set to supplier_inquiry_id while
-- the case is not CLOSED, NULL when closed). MariaDB rejects generated CASE/IF
-- over CHAR(36); MySQL 8 unique NULLs still allow many closed cases.

CREATE TABLE app_users (
    id CHAR(36) NOT NULL PRIMARY KEY,
    external_subject VARCHAR(128) NOT NULL,
    email_normalized VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT app_users_external_unique UNIQUE (external_subject),
    CONSTRAINT app_users_email_unique UNIQUE (email_normalized),
    CONSTRAINT app_users_status_valid CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
    id CHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT roles_code_unique UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    assigned_by_user_id CHAR(36),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT user_roles_user_fk FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE RESTRICT,
    CONSTRAINT user_roles_role_fk FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,
    CONSTRAINT user_roles_assigned_by_fk FOREIGN KEY (assigned_by_user_id) REFERENCES app_users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (id, code, name, description)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'ADMIN', 'Administrator', 'Full administrative access.'),
    ('00000000-0000-4000-8000-000000000002', 'SUPPLIER_REVIEWER', 'Supplier reviewer', 'Reviews and approves supplier candidates.'),
    ('00000000-0000-4000-8000-000000000003', 'MARKETING', 'Marketing user', 'Works purchase leads and records follow-up.'),
    ('00000000-0000-4000-8000-000000000004', 'EXPORTER', 'Export user', 'Creates controlled lead exports.'),
    ('00000000-0000-4000-8000-000000000005', 'TAXONOMY_MANAGER', 'Taxonomy manager', 'Manages departments, product types, products, and standards.');

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
    ADD COLUMN assigned_to_user_id CHAR(36) NULL,
    ADD COLUMN approved_at DATETIME(6) NULL,
    ADD COLUMN approved_by_user_id CHAR(36) NULL,
    ADD CONSTRAINT supplier_assigned_fk FOREIGN KEY (assigned_to_user_id) REFERENCES app_users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT supplier_approved_by_fk FOREIGN KEY (approved_by_user_id) REFERENCES app_users(id) ON DELETE RESTRICT,
    ADD CONSTRAINT supplier_inquiries_approval_valid CHECK (
        (review_state = 'APPROVED' AND approved_at IS NOT NULL AND approved_by_user_id IS NOT NULL)
        OR (
            review_state <> 'APPROVED'
            AND approved_at IS NULL
            AND approved_by_user_id IS NULL
        )
    );

ALTER TABLE purchase_inquiries
    ADD COLUMN assigned_to_user_id CHAR(36) NULL,
    ADD COLUMN first_dispatched_at DATETIME(6) NULL,
    ADD COLUMN marketing_notes TEXT,
    ADD CONSTRAINT purchase_assigned_fk FOREIGN KEY (assigned_to_user_id) REFERENCES app_users(id) ON DELETE RESTRICT;

CREATE TABLE review_cases (
    id CHAR(36) NOT NULL PRIMARY KEY,
    supplier_inquiry_id CHAR(36) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    assigned_to_user_id CHAR(36),
    opened_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    closed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    open_supplier_key CHAR(36) NULL,
    CONSTRAINT review_cases_state_valid CHECK (state IN ('OPEN', 'IN_REVIEW', 'WAITING_FOR_INFO', 'CLOSED')),
    CONSTRAINT review_cases_closed_state_valid CHECK (
        (state = 'CLOSED' AND closed_at IS NOT NULL)
        OR (state <> 'CLOSED' AND closed_at IS NULL)
    ),
    CONSTRAINT review_cases_one_open UNIQUE (open_supplier_key),
    CONSTRAINT review_cases_supplier_fk FOREIGN KEY (supplier_inquiry_id) REFERENCES supplier_inquiries(inquiry_id) ON DELETE RESTRICT,
    CONSTRAINT review_cases_assigned_fk FOREIGN KEY (assigned_to_user_id) REFERENCES app_users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE review_decisions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    review_case_id CHAR(36) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    reason_code VARCHAR(64),
    notes TEXT,
    decided_by_user_id CHAR(36) NOT NULL,
    decided_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT review_decisions_decision_valid CHECK (decision IN (
        'REQUEST_INFORMATION',
        'APPROVE',
        'REJECT'
    )),
    CONSTRAINT review_decisions_case_fk FOREIGN KEY (review_case_id) REFERENCES review_cases(id) ON DELETE RESTRICT,
    CONSTRAINT review_decisions_actor_fk FOREIGN KEY (decided_by_user_id) REFERENCES app_users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE export_jobs (
    id CHAR(36) NOT NULL PRIMARY KEY,
    requested_by_user_id CHAR(36) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    filter_summary JSON NOT NULL DEFAULT ('{}'),
    state VARCHAR(16) NOT NULL DEFAULT 'QUEUED',
    storage_key VARCHAR(512),
    original_filename VARCHAR(255),
    media_type VARCHAR(128),
    byte_size BIGINT,
    expires_at DATETIME(6) NOT NULL,
    generated_at DATETIME(6),
    failure_reason TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT export_jobs_scope_valid CHECK (scope IN ('PURCHASE_LEADS', 'SUPPLIER_INQUIRIES')),
    CONSTRAINT export_jobs_state_valid CHECK (state IN ('QUEUED', 'GENERATING', 'READY', 'FAILED', 'EXPIRED')),
    CONSTRAINT export_jobs_ready_state_valid CHECK (
        (state = 'READY' AND storage_key IS NOT NULL AND generated_at IS NOT NULL)
        OR state <> 'READY'
    ),
    CONSTRAINT export_jobs_requester_fk FOREIGN KEY (requested_by_user_id) REFERENCES app_users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE workflow_events
    ADD COLUMN actor_user_id CHAR(36) NULL,
    ADD CONSTRAINT workflow_events_actor_user_fk FOREIGN KEY (actor_user_id) REFERENCES app_users(id) ON DELETE RESTRICT;

ALTER TABLE audit_events
    ADD CONSTRAINT audit_events_actor_user_fk
    FOREIGN KEY (actor_user_id) REFERENCES app_users(id) ON DELETE RESTRICT;

CREATE INDEX supplier_inquiries_review_state_idx
    ON supplier_inquiries (review_state, updated_at);
CREATE INDEX purchase_inquiries_lead_state_idx
    ON purchase_inquiries (lead_state, updated_at);
CREATE INDEX review_decisions_case_idx
    ON review_decisions (review_case_id, decided_at);
CREATE INDEX export_jobs_requester_idx
    ON export_jobs (requested_by_user_id, created_at);
