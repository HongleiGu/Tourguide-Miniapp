-- MIN-12 baseline schema (skeleton). Columns are intentionally minimal and will be
-- extended per feature epic via new Vn__ migrations. InnoDB + utf8mb4 throughout.
-- Money is stored in fen (分) as BIGINT to avoid floating-point rounding.

-- ---------- accounts & RBAC ----------
CREATE TABLE app_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    open_id     VARCHAR(64)  NOT NULL,
    union_id    VARCHAR(64)  NULL,
    phone       VARCHAR(20)  NULL,
    nickname    VARCHAR(64)  NULL,
    avatar_url  VARCHAR(512) NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_user_open_id (open_id),
    KEY idx_app_user_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role (
    id    BIGINT      NOT NULL AUTO_INCREMENT,
    code  VARCHAR(32) NOT NULL,
    name  VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role (role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- guides ----------
CREATE TABLE guide_profile (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    employment_type  VARCHAR(16)  NOT NULL DEFAULT 'SELF',     -- SELF 自营 / OUTSOURCED 外包
    bio              TEXT         NULL,
    dispatch_weight  INT          NOT NULL DEFAULT 100,        -- 派单权重 (admin-set, MIN-7)
    rating           DECIMAL(3,2) NOT NULL DEFAULT 5.00,
    star_level       TINYINT      NOT NULL DEFAULT 5,
    reject_rate      DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    accepting_orders TINYINT(1)   NOT NULL DEFAULT 1,          -- 开启/关闭接单
    status           VARCHAR(16)  NOT NULL DEFAULT 'ENABLED',  -- ENABLED / DISABLED / SUSPENDED
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_guide_user (user_id),
    CONSTRAINT fk_guide_user FOREIGN KEY (user_id) REFERENCES app_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- sessions (场次) ----------
CREATE TABLE scenic_session (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    title        VARCHAR(128) NOT NULL,
    type         VARCHAR(16) NOT NULL,                 -- PRIVATE 私讲 / GROUP 拼团 / EXCLUSIVE 专属时段
    session_date DATE        NOT NULL,
    start_time   TIME        NULL,
    end_time     TIME        NULL,
    capacity     INT         NOT NULL DEFAULT 1,
    guide_id     BIGINT      NULL,
    price_fen    BIGINT      NOT NULL DEFAULT 0,
    status       VARCHAR(16) NOT NULL DEFAULT 'OPEN',  -- OPEN / LOCKED 锁场 / CLOSED 停场 / CANCELLED
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_session_date_type (session_date, type),
    KEY idx_session_guide (guide_id),
    CONSTRAINT fk_session_guide FOREIGN KEY (guide_id) REFERENCES guide_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- group-buy state (拼团, MIN-4) ----------
CREATE TABLE session_group_buy (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    session_id   BIGINT      NOT NULL,
    min_size     INT         NOT NULL DEFAULT 2,           -- 成团最低人数
    max_size     INT         NOT NULL DEFAULT 10,          -- 最大人数
    current_size INT         NOT NULL DEFAULT 0,           -- 已拼人数 (authoritative count; Redis is the hot cache)
    status       VARCHAR(16) NOT NULL DEFAULT 'FORMING',   -- FORMING / LOCKED 满员锁单 / COMPLETED / VOIDED 超时作废
    deadline     TIMESTAMP   NULL,                         -- 超时未服务作废
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_buy_session (session_id),
    CONSTRAINT fk_group_buy_session FOREIGN KEY (session_id) REFERENCES scenic_session (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- schedule (排班, MIN-6) ----------
CREATE TABLE guide_schedule (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    guide_id   BIGINT      NOT NULL,
    work_date  DATE        NOT NULL,
    start_time TIME        NULL,
    end_time   TIME        NULL,
    type       VARCHAR(16) NOT NULL DEFAULT 'WORK',   -- WORK 上班 / REST 休息
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_schedule_guide_date (guide_id, work_date),
    CONSTRAINT fk_schedule_guide FOREIGN KEY (guide_id) REFERENCES guide_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- orders ----------
CREATE TABLE booking_order (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    order_no      VARCHAR(32) NOT NULL,
    user_id       BIGINT      NOT NULL,
    session_id    BIGINT      NOT NULL,
    guide_id      BIGINT      NULL,
    type          VARCHAR(16) NOT NULL,                       -- PRIVATE / GROUP / EXCLUSIVE
    people_count  INT         NOT NULL DEFAULT 1,
    contact_name  VARCHAR(64) NULL,
    contact_phone VARCHAR(20) NULL,
    visit_date    DATE        NULL,
    amount_fen    BIGINT      NOT NULL DEFAULT 0,
    status        VARCHAR(24) NOT NULL DEFAULT 'PENDING_PAYMENT',
                  -- PENDING_PAYMENT / PAID 待服务 / COMPLETED 已完成 / CANCELLED 已取消 / REFUNDED 退款
    verify_code   VARCHAR(64) NULL,                           -- 核销码
    paid_at       TIMESTAMP   NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_user (user_id),
    KEY idx_order_session (session_id),
    KEY idx_order_guide (guide_id),
    KEY idx_order_status (status),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_order_session FOREIGN KEY (session_id) REFERENCES scenic_session (id),
    CONSTRAINT fk_order_guide FOREIGN KEY (guide_id) REFERENCES guide_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- verification (核销, MIN-5). order_id UNIQUE => 不可重复核销 ----------
CREATE TABLE order_verification (
    id          BIGINT    NOT NULL AUTO_INCREMENT,
    order_id    BIGINT    NOT NULL,
    guide_id    BIGINT    NOT NULL,
    verified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_verification_order (order_id),
    KEY idx_verification_guide (guide_id),
    CONSTRAINT fk_verification_order FOREIGN KEY (order_id) REFERENCES booking_order (id),
    CONSTRAINT fk_verification_guide FOREIGN KEY (guide_id) REFERENCES guide_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- reviews (评价, MIN-5) ----------
CREATE TABLE order_review (
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    order_id   BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    guide_id   BIGINT    NOT NULL,
    rating     TINYINT   NOT NULL,                  -- 评分 1-5
    content    TEXT      NULL,                       -- 文字评价
    images     JSON      NULL,                       -- 图片评价 (url array)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_order (order_id),
    KEY idx_review_guide (guide_id),
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES booking_order (id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_review_guide FOREIGN KEY (guide_id) REFERENCES guide_profile (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------- seed the role catalogue (MIN-2 RBAC) ----------
INSERT INTO role (code, name) VALUES
    ('TOURIST',       '游客'),
    ('GUIDE',         '讲解员'),
    ('ADMIN_SUPER',   '超级管理员'),
    ('ADMIN_OPS',     '运营管理员'),
    ('ADMIN_FINANCE', '财务管理员');
