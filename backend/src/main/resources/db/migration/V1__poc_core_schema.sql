-- POC core schema. Scan-first fixes vs the approved singleton DDL:
-- * inquiries.route nullable until the visitor chooses sell/buy
-- * inquiry_parties.company_name_submitted nullable (buyer company is optional)
-- * EXHIBITION_QR still requires qr_campaign_id (seeded in V2)
-- Consent uniqueness vs revocation is deferred (no consent tables in this POC).

CREATE SCHEMA IF NOT EXISTS exhibition_portal;
SET search_path TO exhibition_portal, public;

CREATE TABLE exhibitions (
    id uuid PRIMARY KEY,
    name text NOT NULL,
    starts_at timestamptz NOT NULL,
    ends_at timestamptz NOT NULL,
    timezone_name text NOT NULL,
    venue_name text,
    status text NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED', 'ARCHIVED')),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT exhibitions_time_range_valid CHECK (ends_at > starts_at)
);

CREATE TABLE qr_campaigns (
    id uuid PRIMARY KEY,
    exhibition_id uuid REFERENCES exhibitions(id) ON DELETE RESTRICT,
    code text NOT NULL UNIQUE,
    label text NOT NULL,
    landing_route text NOT NULL DEFAULT 'CHOICE'
        CHECK (landing_route IN ('CHOICE', 'SUPPLIER', 'PURCHASE')),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE departments (
    id uuid PRIMARY KEY,
    code varchar(50) NOT NULL UNIQUE,
    name text NOT NULL,
    display_order integer NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE product_types (
    id uuid PRIMARY KEY,
    code varchar(50) NOT NULL UNIQUE,
    name text NOT NULL,
    display_order integer NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE department_product_types (
    department_id uuid NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    product_type_id uuid NOT NULL REFERENCES product_types(id) ON DELETE RESTRICT,
    display_order integer NOT NULL DEFAULT 0,
    is_active boolean NOT NULL DEFAULT true,
    PRIMARY KEY (department_id, product_type_id)
);

CREATE TABLE pharmacopoeial_standards (
    id uuid PRIMARY KEY,
    code varchar(16) NOT NULL UNIQUE,
    name text NOT NULL,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inquiries (
    id uuid PRIMARY KEY,
    reference_code text NOT NULL UNIQUE,
    route text CHECK (route IS NULL OR route IN ('SUPPLIER', 'PURCHASE')),
    entry_channel text NOT NULL
        CHECK (entry_channel IN ('EXHIBITION_QR', 'WEBSITE', 'DIRECT')),
    qr_campaign_id uuid REFERENCES qr_campaigns(id) ON DELETE RESTRICT,
    exhibition_id uuid REFERENCES exhibitions(id) ON DELETE RESTRICT,
    lifecycle_state text NOT NULL DEFAULT 'DRAFT'
        CHECK (lifecycle_state IN (
            'DRAFT', 'SUBMITTED', 'CANCELLED', 'ARCHIVED', 'RETENTION_PURGED'
        )),
    submitted_at timestamptz,
    ui_step text NOT NULL DEFAULT 'card-capture',
    contact_confirmed boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inquiries_submission_state_valid CHECK (
        (lifecycle_state = 'SUBMITTED' AND submitted_at IS NOT NULL AND route IS NOT NULL)
        OR lifecycle_state <> 'SUBMITTED'
    ),
    CONSTRAINT inquiries_qr_channel_valid CHECK (
        entry_channel <> 'EXHIBITION_QR' OR qr_campaign_id IS NOT NULL
    )
);

CREATE TABLE inquiry_parties (
    id uuid PRIMARY KEY,
    inquiry_id uuid NOT NULL REFERENCES inquiries(id) ON DELETE RESTRICT,
    role text NOT NULL CHECK (role IN ('SUPPLIER_CONTACT', 'BUYER_CONTACT')),
    company_name_submitted text,
    person_name_submitted text NOT NULL,
    email_submitted text NOT NULL,
    email_normalized text NOT NULL,
    phone_submitted text,
    phone_e164 varchar(32),
    job_title_submitted text,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (inquiry_id, role)
);

CREATE TABLE supplier_inquiries (
    inquiry_id uuid PRIMARY KEY REFERENCES inquiries(id) ON DELETE RESTRICT,
    review_state text NOT NULL DEFAULT 'DRAFT'
        CHECK (review_state IN (
            'DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'NEEDS_INFORMATION', 'APPROVED', 'REJECTED'
        )),
    production_state text NOT NULL DEFAULT 'NOT_REQUESTED'
        CHECK (production_state IN (
            'NOT_REQUESTED', 'QUEUED', 'IN_PROGRESS', 'SUCCEEDED', 'FAILED'
        )),
    website_url text,
    catalogue_filename text,
    catalogue_media_type text,
    catalogue_byte_size bigint,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE supplier_inquiry_departments (
    inquiry_id uuid NOT NULL REFERENCES supplier_inquiries(inquiry_id) ON DELETE CASCADE,
    department_id uuid NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (inquiry_id, department_id)
);

CREATE TABLE supplier_inquiry_product_types (
    inquiry_id uuid NOT NULL,
    department_id uuid NOT NULL,
    product_type_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (inquiry_id, department_id, product_type_id),
    CONSTRAINT supplier_product_type_selected_department_fk
        FOREIGN KEY (inquiry_id, department_id)
        REFERENCES supplier_inquiry_departments(inquiry_id, department_id)
        ON DELETE CASCADE,
    CONSTRAINT supplier_product_type_department_mapping_fk
        FOREIGN KEY (department_id, product_type_id)
        REFERENCES department_product_types(department_id, product_type_id)
        ON DELETE RESTRICT
);

CREATE TABLE purchase_inquiries (
    inquiry_id uuid PRIMARY KEY REFERENCES inquiries(id) ON DELETE RESTRICT,
    lead_state text NOT NULL DEFAULT 'DRAFT'
        CHECK (lead_state IN (
            'DRAFT', 'SUBMITTED', 'QUEUED', 'DISPATCHED', 'DELIVERY_FAILED',
            'IN_PROGRESS', 'CLOSED', 'DISQUALIFIED'
        )),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE purchase_line_items (
    id uuid PRIMARY KEY,
    purchase_inquiry_id uuid NOT NULL REFERENCES purchase_inquiries(inquiry_id) ON DELETE CASCADE,
    requirement_text text,
    quantity_text text,
    pack_size_text text,
    needed_by_date text,
    notes text,
    product_area_search text,
    standard_code varchar(16),
    display_order integer NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE workflow_events (
    id uuid PRIMARY KEY,
    inquiry_id uuid NOT NULL REFERENCES inquiries(id) ON DELETE RESTRICT,
    workflow text NOT NULL,
    from_state text,
    to_state text NOT NULL,
    actor_kind text NOT NULL DEFAULT 'VISITOR'
        CHECK (actor_kind IN ('VISITOR', 'USER', 'SYSTEM', 'INTEGRATION')),
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inquiry_ui_state (
    inquiry_id uuid PRIMARY KEY REFERENCES inquiries(id) ON DELETE CASCADE,
    card_front_name text,
    card_front_size bigint,
    card_front_type text,
    card_back_name text,
    card_back_size bigint,
    card_back_type text,
    card_qr_payload_internal text,
    location_from_card text,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX inquiries_lifecycle_idx
    ON inquiries (route, lifecycle_state, submitted_at DESC);
CREATE INDEX inquiry_parties_email_idx
    ON inquiry_parties (email_normalized);
