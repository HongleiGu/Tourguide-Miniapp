-- 价格与拼团规则 (MIN-49): per session-type x day-type (工作日/节假日) pricing + group min/max.
CREATE TABLE pricing_rule (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    session_type VARCHAR(16) NOT NULL,                 -- PRIVATE / GROUP / EXCLUSIVE
    day_type     VARCHAR(16) NOT NULL,                 -- WORKDAY 工作日 / HOLIDAY 节假日
    price_fen    BIGINT      NOT NULL DEFAULT 0,
    group_min    INT         NULL,                     -- 成团最低人数 (GROUP)
    group_max    INT         NULL,                     -- 最大人数 (GROUP)
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pricing (session_type, day_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO pricing_rule (session_type, day_type, price_fen, group_min, group_max) VALUES
    ('PRIVATE',   'WORKDAY', 30000, NULL, NULL),
    ('PRIVATE',   'HOLIDAY', 36000, NULL, NULL),
    ('GROUP',     'WORKDAY',  8000, 2,    10),
    ('GROUP',     'HOLIDAY', 10000, 2,    10),
    ('EXCLUSIVE', 'WORKDAY', 12000, NULL, NULL),
    ('EXCLUSIVE', 'HOLIDAY', 15000, NULL, NULL);
