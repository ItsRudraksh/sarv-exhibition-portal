-- Buyer catalogue is not only pharma-erp finished goods.
-- Offline suppliers and portal-registered suppliers can list named products for buyers.
-- listed_for_buyers is the explicit tag; deactivate rather than delete when inquiries reference a product.

CREATE TABLE trading_suppliers (
    id CHAR(36) NOT NULL PRIMARY KEY,
    source_kind VARCHAR(32) NOT NULL,
    portal_inquiry_id CHAR(36) NULL,
    company_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(64) NULL,
    website_url VARCHAR(512) NULL,
    notes TEXT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by_user_id CHAR(36) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT trading_suppliers_source_valid CHECK (source_kind IN ('OFFLINE', 'PORTAL')),
    CONSTRAINT trading_suppliers_status_valid CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT trading_suppliers_source_shape_valid CHECK (
        (source_kind = 'OFFLINE' AND portal_inquiry_id IS NULL)
        OR (source_kind = 'PORTAL' AND portal_inquiry_id IS NOT NULL)
    ),
    CONSTRAINT trading_suppliers_portal_unique UNIQUE (portal_inquiry_id),
    CONSTRAINT trading_suppliers_portal_fk
        FOREIGN KEY (portal_inquiry_id) REFERENCES supplier_inquiries(inquiry_id) ON DELETE RESTRICT,
    CONSTRAINT trading_suppliers_created_by_fk
        FOREIGN KEY (created_by_user_id) REFERENCES app_users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE trading_products (
    id CHAR(36) NOT NULL PRIMARY KEY,
    supplier_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    listed_for_buyers TINYINT(1) NOT NULL DEFAULT 0,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT trading_products_supplier_fk
        FOREIGN KEY (supplier_id) REFERENCES trading_suppliers(id) ON DELETE RESTRICT,
    CONSTRAINT trading_products_name_unique UNIQUE (supplier_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX trading_products_listed_idx
    ON trading_products (listed_for_buyers, is_active, name);

CREATE TABLE purchase_inquiry_trading_products (
    inquiry_id CHAR(36) NOT NULL,
    trading_product_id CHAR(36) NOT NULL,
    quantity_text VARCHAR(128) NOT NULL DEFAULT '',
    PRIMARY KEY (inquiry_id, trading_product_id),
    CONSTRAINT pitp_inquiry_fk FOREIGN KEY (inquiry_id) REFERENCES purchase_inquiries(inquiry_id) ON DELETE CASCADE,
    CONSTRAINT pitp_product_fk FOREIGN KEY (trading_product_id) REFERENCES trading_products(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
