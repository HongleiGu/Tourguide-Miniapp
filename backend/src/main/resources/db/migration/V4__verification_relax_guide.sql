-- 核销 (MIN-33): the verifier is an authenticated GUIDE app-user, not yet a guide_profile
-- (guide profiles arrive with the guide app / admin in MIN-6 / MIN-8). Relax guide_id so a
-- verification can record which user performed it without requiring a guide_profile row.
ALTER TABLE order_verification DROP FOREIGN KEY fk_verification_guide;
ALTER TABLE order_verification MODIFY guide_id BIGINT NULL;
-- uk_verification_order (order_id UNIQUE) stays: enforces 不可重复核销.
