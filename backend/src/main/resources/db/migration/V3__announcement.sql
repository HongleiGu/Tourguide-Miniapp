-- MIN-23: tourist-facing announcements (公告) shown on the home page.
CREATE TABLE announcement (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(128) NOT NULL,
    content    TEXT         NULL,
    type       VARCHAR(32)  NULL,                       -- 限流 / 开放时间 / 停服 ...
    active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_announcement_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
