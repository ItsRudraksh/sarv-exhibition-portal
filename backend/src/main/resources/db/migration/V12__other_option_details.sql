-- Other-option free-text details for sell categories/types and buy catalogue.

ALTER TABLE supplier_inquiries
    ADD COLUMN other_category_detail TEXT NULL,
    ADD COLUMN other_product_type_detail TEXT NULL;

ALTER TABLE purchase_inquiries
    ADD COLUMN other_product TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN other_product_detail TEXT NULL;
