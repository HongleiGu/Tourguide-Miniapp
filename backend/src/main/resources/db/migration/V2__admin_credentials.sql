-- MIN-19: admins live in the same app_user table, keyed by username + bcrypt password.
-- WeChat users have open_id (no password); admins have username/password_hash (no open_id),
-- so open_id becomes nullable. MySQL allows multiple NULLs under a unique index.

ALTER TABLE app_user
    MODIFY open_id VARCHAR(64) NULL;

ALTER TABLE app_user
    ADD COLUMN username      VARCHAR(64)  NULL AFTER union_id,
    ADD COLUMN password_hash VARCHAR(100) NULL AFTER username,
    ADD UNIQUE KEY uk_app_user_username (username);
