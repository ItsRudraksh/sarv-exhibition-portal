-- POC core schema (MySQL 8 / MariaDB 10+). Scan-first fixes vs the historical PostgreSQL singleton DDL:
-- * inquiries.route nullable until the visitor chooses sell/buy
-- * inquiry_parties.company_name_submitted nullable (buyer company is optional)
-- * EXHIBITION_QR still requires qr_campaign_id (seeded in V2)
-- UUIDs are CHAR(36). Applied store is MySQL, not PostgreSQL.

CREATE TABLE exhibitions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    timezone_name VARCHAR(64) NOT NULL,
    venue_name VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT exhibitions_status_valid CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT exhibitions_time_range_valid CHECK (ends_at > starts_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE qr_campaigns (
    id CHAR(36) NOT NULL PRIMARY KEY,
    exhibition_id CHAR(36),
    code VARCHAR(64) NOT NULL,
    label VARCHAR(255) NOT NULL,
    landing_route VARCHAR(32) NOT NULL DEFAULT 'CHOICE',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT qr_campaigns_code_unique UNIQUE (code),
    CONSTRAINT qr_campaigns_landing_valid CHECK (landing_route IN ('CHOICE', 'SUPPLIER', 'PURCHASE')),
    CONSTRAINT qr_campaigns_exhibition_fk FOREIGN KEY (exhibition_id) REFERENCES exhibitions(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE departments (
    id CHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT departments_code_unique UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_types (
    id CHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT product_types_code_unique UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE department_product_types (
    department_id CHAR(36) NOT NULL,
    product_type_id CHAR(36) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (department_id, product_type_id),
    CONSTRAINT dpt_department_fk FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT,
    CONSTRAINT dpt_product_type_fk FOREIGN KEY (product_type_id) REFERENCES product_types(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pharmacopoeial_standards (
    id CHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pharmacopoeial_standards_code_unique UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inquiries (
    id CHAR(36) NOT NULL PRIMARY KEY,
    reference_code VARCHAR(64) NOT NULL,
    route VARCHAR(32),
    entry_channel VARCHAR(32) NOT NULL,
    qr_campaign_id CHAR(36),
    exhibition_id CHAR(36),
    lifecycle_state VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    submitted_at DATETIME(6),
    ui_step VARCHAR(64) NOT NULL DEFAULT 'card-capture',
    contact_confirmed TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT inquiries_reference_unique UNIQUE (reference_code),
    CONSTRAINT inquiries_route_valid CHECK (route IS NULL OR route IN ('SUPPLIER', 'PURCHASE')),
    CONSTRAINT inquiries_entry_valid CHECK (entry_channel IN ('EXHIBITION_QR', 'WEBSITE', 'DIRECT')),
    CONSTRAINT inquiries_lifecycle_valid CHECK (lifecycle_state IN (
        'DRAFT', 'SUBMITTED', 'CANCELLED', 'ARCHIVED', 'RETENTION_PURGED'
    )),
    CONSTRAINT inquiries_submission_state_valid CHECK (
        (lifecycle_state = 'SUBMITTED' AND submitted_at IS NOT NULL AND route IS NOT NULL)
        OR lifecycle_state <> 'SUBMITTED'
    ),
    CONSTRAINT inquiries_qr_channel_valid CHECK (
        entry_channel <> 'EXHIBITION_QR' OR qr_campaign_id IS NOT NULL
    ),
    CONSTRAINT inquiries_qr_fk FOREIGN KEY (qr_campaign_id) REFERENCES qr_campaigns(id) ON DELETE RESTRICT,
    CONSTRAINT inquiries_exhibition_fk FOREIGN KEY (exhibition_id) REFERENCES exhibitions(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inquiry_parties (
    id CHAR(36) NOT NULL PRIMARY KEY,
    inquiry_id CHAR(36) NOT NULL,
    role VARCHAR(32) NOT NULL,
    company_name_submitted VARCHAR(255),
    person_name_submitted VARCHAR(255) NOT NULL,
    email_submitted VARCHAR(255) NOT NULL,
    email_normalized VARCHAR(255) NOT NULL,
    phone_submitted VARCHAR(64),
    phone_e164 VARCHAR(32),
    job_title_submitted VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT inquiry_parties_role_valid CHECK (role IN ('SUPPLIER_CONTACT', 'BUYER_CONTACT')),
    CONSTRAINT inquiry_parties_inquiry_role UNIQUE (inquiry_id, role),
    CONSTRAINT inquiry_parties_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supplier_inquiries (
    inquiry_id CHAR(36) NOT NULL PRIMARY KEY,
    review_state VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    production_state VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED',
    website_url VARCHAR(512),
    catalogue_filename VARCHAR(255),
    catalogue_media_type VARCHAR(128),
    catalogue_byte_size BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT supplier_review_state_valid CHECK (review_state IN (
        'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'NEEDS_INFORMATION', 'APPROVED', 'REJECTED'
    )),
    CONSTRAINT supplier_production_state_valid CHECK (production_state IN (
        'NOT_REQUESTED', 'QUEUED', 'IN_PROGRESS', 'SUCCEEDED', 'FAILED'
    )),
    CONSTRAINT supplier_inquiries_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supplier_inquiry_departments (
    inquiry_id CHAR(36) NOT NULL,
    department_id CHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (inquiry_id, department_id),
    CONSTRAINT sid_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES supplier_inquiries(inquiry_id) ON DELETE CASCADE,
    CONSTRAINT sid_department_fk FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supplier_inquiry_product_types (
    inquiry_id CHAR(36) NOT NULL,
    department_id CHAR(36) NOT NULL,
    product_type_id CHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (inquiry_id, department_id, product_type_id),
    CONSTRAINT sipt_selected_department_fk
        FOREIGN KEY (inquiry_id, department_id)
        REFERENCES supplier_inquiry_departments(inquiry_id, department_id)
        ON DELETE CASCADE,
    CONSTRAINT sipt_department_mapping_fk
        FOREIGN KEY (department_id, product_type_id)
        REFERENCES department_product_types(department_id, product_type_id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE purchase_inquiries (
    inquiry_id CHAR(36) NOT NULL PRIMARY KEY,
    lead_state VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT purchase_lead_state_valid CHECK (lead_state IN (
        'DRAFT', 'SUBMITTED', 'QUEUED', 'DISPATCHED', 'DELIVERY_FAILED',
        'IN_PROGRESS', 'CLOSED', 'DISQUALIFIED'
    )),
    CONSTRAINT purchase_inquiries_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE purchase_line_items (
    id CHAR(36) NOT NULL PRIMARY KEY,
    purchase_inquiry_id CHAR(36) NOT NULL,
    requirement_text TEXT,
    quantity_text VARCHAR(128),
    pack_size_text VARCHAR(128),
    needed_by_date VARCHAR(64),
    notes TEXT,
    product_area_search VARCHAR(255),
    standard_code VARCHAR(16),
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pli_purchase_fk FOREIGN KEY (purchase_inquiry_id) REFERENCES purchase_inquiries(inquiry_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workflow_events (
    id CHAR(36) NOT NULL PRIMARY KEY,
    inquiry_id CHAR(36) NOT NULL,
    workflow VARCHAR(64) NOT NULL,
    from_state VARCHAR(64),
    to_state VARCHAR(64) NOT NULL,
    actor_kind VARCHAR(32) NOT NULL DEFAULT 'VISITOR',
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT workflow_events_actor_valid CHECK (actor_kind IN ('VISITOR', 'USER', 'SYSTEM', 'INTEGRATION')),
    CONSTRAINT workflow_events_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inquiry_ui_state (
    inquiry_id CHAR(36) NOT NULL PRIMARY KEY,
    card_front_name VARCHAR(255),
    card_front_size BIGINT,
    card_front_type VARCHAR(128),
    card_back_name VARCHAR(255),
    card_back_size BIGINT,
    card_back_type VARCHAR(128),
    card_qr_payload_internal TEXT,
    location_from_card VARCHAR(255),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT inquiry_ui_state_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES inquiries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX inquiries_lifecycle_idx ON inquiries (route, lifecycle_state, submitted_at);
CREATE INDEX inquiry_parties_email_idx ON inquiry_parties (email_normalized);
