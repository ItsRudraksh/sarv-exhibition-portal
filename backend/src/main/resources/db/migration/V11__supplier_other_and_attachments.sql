-- Supplier offerings that do not fit listed departments/product types, plus
-- supporting attachments for both sell and buy (purpose INQUIRY_ATTACHMENT).

ALTER TABLE supplier_inquiries
    ADD COLUMN other_category TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN other_product_type TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN capability_notes TEXT NULL;

ALTER TABLE file_assets DROP CONSTRAINT file_assets_purpose_valid;

ALTER TABLE file_assets
    ADD CONSTRAINT file_assets_purpose_valid CHECK (purpose IN (
        'CATALOGUE_ORIGINAL',
        'CATALOGUE_DERIVED_PDF',
        'BUSINESS_CARD',
        'VOICE_INPUT',
        'EXCEL_EXPORT',
        'INQUIRY_ATTACHMENT'
    ));
