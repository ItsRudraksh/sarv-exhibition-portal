-- Per finished-good quantity on buyer multi-select.

ALTER TABLE purchase_inquiry_finished_goods
    ADD COLUMN quantity_text VARCHAR(128) NOT NULL DEFAULT '' AFTER finished_good_id;
