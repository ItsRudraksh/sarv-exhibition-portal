-- Finished goods snapshot (pharma-erp products / OUTPUT_PRODUCTS) for buyer multi-select.

CREATE TABLE finished_goods (
    id CHAR(36) NOT NULL PRIMARY KEY,
    external_id BIGINT NOT NULL,
    code VARCHAR(64),
    name VARCHAR(255) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    display_order INTEGER NOT NULL DEFAULT 0,
    synced_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT finished_goods_external_unique UNIQUE (external_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX finished_goods_active_name_idx ON finished_goods (is_active, name);

CREATE TABLE purchase_inquiry_finished_goods (
    inquiry_id CHAR(36) NOT NULL,
    finished_good_id CHAR(36) NOT NULL,
    PRIMARY KEY (inquiry_id, finished_good_id),
    CONSTRAINT pifg_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES purchase_inquiries(inquiry_id) ON DELETE CASCADE,
    CONSTRAINT pifg_fg_fk FOREIGN KEY (finished_good_id) REFERENCES finished_goods(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE finished_goods_sync_runs (
    id CHAR(36) NOT NULL PRIMARY KEY,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6),
    state VARCHAR(32) NOT NULL,
    rows_upserted INTEGER NOT NULL DEFAULT 0,
    rows_deactivated INTEGER NOT NULL DEFAULT 0,
    message VARCHAR(512),
    CONSTRAINT fgsr_state_valid CHECK (state IN ('RUNNING', 'SUCCEEDED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
