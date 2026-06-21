-- 评价 (MIN-34): the reviewed guide may not have a guide_profile yet (profiles arrive with
-- the guide app / dispatch in MIN-6 / MIN-7). Relax guide_id so a review can be stored against
-- the order's guide id (nullable) without requiring a guide_profile row.
ALTER TABLE order_review DROP FOREIGN KEY fk_review_guide;
ALTER TABLE order_review MODIFY guide_id BIGINT NULL;
-- uk_review_order (order_id UNIQUE) stays: one review per order.
