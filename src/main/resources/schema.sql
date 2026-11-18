-- Gooday 전체 스키마. 서버 시작 시마다 실행되므로 모두 IF NOT EXISTS.

-- ───────── 공통 ─────────
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    timezone    VARCHAR(40)  NOT NULL DEFAULT 'Asia/Seoul',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ───────── 1. 대시보드 ─────────
CREATE TABLE IF NOT EXISTS ddays (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(100) NOT NULL,
    category    VARCHAR(20)  NOT NULL DEFAULT 'ETC',      -- EXAM, BJJ, PAYDAY, CARD, ETC
    target_date DATE         NOT NULL,
    repeat_type VARCHAR(10)  NOT NULL DEFAULT 'NONE',     -- NONE, MONTHLY, YEARLY
    pinned      BOOLEAN      NOT NULL DEFAULT FALSE,
    memo        VARCHAR(500),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ddays_user_date (user_id, target_date),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS routines (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(100) NOT NULL,
    category    VARCHAR(20)  NOT NULL DEFAULT 'LIFE',     -- STUDY, EXERCISE, FINANCE, LIFE
    days_mask   TINYINT      NOT NULL DEFAULT 127,        -- 월=1 화=2 수=4 목=8 금=16 토=32 일=64
    sort_order  INT          NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    start_date  DATE         NOT NULL,                    -- 달성률 계산 시작일
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS routine_checks (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    routine_id  BIGINT   NOT NULL,
    check_date  DATE     NOT NULL,
    checked_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_routine_date (routine_id, check_date),
    FOREIGN KEY (routine_id) REFERENCES routines(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS goals (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    title        VARCHAR(100)  NOT NULL,
    category     VARCHAR(20)   NOT NULL DEFAULT 'LIFE',
    period_type  VARCHAR(10)   NOT NULL,                  -- DAILY, WEEKLY, MONTHLY, YEARLY
    target_value DECIMAL(10,2) NOT NULL,
    unit         VARCHAR(20)   NOT NULL DEFAULT '회',
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS goal_progress (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    goal_id    BIGINT        NOT NULL,
    log_date   DATE          NOT NULL,
    value      DECIMAL(10,2) NOT NULL,
    memo       VARCHAR(200),
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_goal_date (goal_id, log_date),
    FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ───────── 2. 운동 (주짓수) ─────────
CREATE TABLE IF NOT EXISTS bjj_attendance (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    attend_date     DATE        NOT NULL,
    class_type      VARCHAR(20) NOT NULL DEFAULT 'GI',    -- GI, NOGI, OPEN_MAT, PRIVATE
    duration_min    INT,
    sparring_rounds INT,
    techniques      VARCHAR(500),
    memo            TEXT,
    created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bjj_att (user_id, attend_date),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bjj_competitions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    name         VARCHAR(100) NOT NULL,
    comp_date    DATE         NOT NULL,
    location     VARCHAR(100),
    division     VARCHAR(50),                             -- 벨트/연령 디비전
    weight_class VARCHAR(30),
    result       VARCHAR(50),                             -- 금/은/동/참가 등
    memo         TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bjj_monthly_goals (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT  NOT NULL,
    ym              CHAR(7) NOT NULL,                     -- '2026-11'
    target_sessions INT     NOT NULL DEFAULT 12,
    focus_technique VARCHAR(200),
    memo            TEXT,
    UNIQUE KEY uk_bjj_goal_ym (user_id, ym),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ───────── 공부 ─────────
CREATE TABLE IF NOT EXISTS study_logs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    study_date DATE        NOT NULL,
    subject    VARCHAR(50) NOT NULL,                      -- JLPT, 코딩 ...
    minutes    INT         NOT NULL,
    content    VARCHAR(500),
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_study (user_id, study_date),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ───────── 3. 자산 / 가계부 ─────────
CREATE TABLE IF NOT EXISTS payment_methods (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    name          VARCHAR(50) NOT NULL,                   -- 'OO카드', '현금', 'OO은행'
    type          VARCHAR(20) NOT NULL,                   -- CASH, BANK, CREDIT_CARD, CHECK_CARD
    statement_day TINYINT,                                -- 신용카드 이용기간 마감일
    billing_day   TINYINT,                                -- 신용카드 결제일
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_pm_name (user_id, name),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tx_categories (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id   BIGINT      NOT NULL,
    type      VARCHAR(10) NOT NULL,                       -- INCOME, EXPENSE
    name      VARCHAR(50) NOT NULL,
    parent_id BIGINT,                                     -- 대분류/소분류
    UNIQUE KEY uk_cat (user_id, type, parent_id, name),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (parent_id) REFERENCES tx_categories(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS import_batches (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    file_name      VARCHAR(200) NOT NULL,
    column_mapping JSON,                                  -- {"날짜":"tx_date","금액":"amount",...}
    row_count      INT          NOT NULL DEFAULT 0,
    skipped_count  INT          NOT NULL DEFAULT 0,
    imported_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS transactions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT        NOT NULL,
    tx_date           DATE          NOT NULL,
    type              VARCHAR(10)   NOT NULL,             -- INCOME, EXPENSE, TRANSFER
    category_id       BIGINT,
    payment_method_id BIGINT,
    amount            DECIMAL(15,2) NOT NULL,
    currency          CHAR(3)       NOT NULL DEFAULT 'KRW',
    description       VARCHAR(200),
    memo              VARCHAR(500),
    source            VARCHAR(10)   NOT NULL DEFAULT 'MANUAL',  -- MANUAL, IMPORT
    import_batch_id   BIGINT,
    row_hash          CHAR(64),                           -- 중복 Import 방지
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tx_hash (user_id, row_hash),
    INDEX idx_tx_date (user_id, tx_date),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES tx_categories(id),
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
    FOREIGN KEY (import_batch_id) REFERENCES import_batches(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS monthly_budgets (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT        NOT NULL,
    ym               CHAR(7)       NOT NULL,
    expected_income  DECIMAL(15,2) NOT NULL DEFAULT 0,
    fixed_expense    DECIMAL(15,2) NOT NULL DEFAULT 0,    -- 월세·통신비·구독 등
    savings_target   DECIMAL(15,2) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_budget_ym (user_id, ym),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS card_bills (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_method_id BIGINT        NOT NULL,
    billing_ym        CHAR(7)       NOT NULL,
    due_date          DATE          NOT NULL,
    amount            DECIMAL(15,2) NOT NULL,
    paid              BOOLEAN       NOT NULL DEFAULT FALSE,
    paid_at           DATETIME,
    UNIQUE KEY uk_bill (payment_method_id, billing_ym),
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ───────── 4. 기록 ─────────
CREATE TABLE IF NOT EXISTS notes (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    note_date  DATE         NOT NULL,
    title      VARCHAR(200),
    content    TEXT         NOT NULL,
    tags       VARCHAR(200),
    pinned     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notes (user_id, note_date),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS daily_reviews (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT  NOT NULL,
    review_date DATE    NOT NULL,
    mood        TINYINT,                                  -- 1~5
    good        TEXT,
    bad         TEXT,
    tomorrow    TEXT,
    UNIQUE KEY uk_review (user_id, review_date),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ───────── 5. 리포트 ─────────
CREATE TABLE IF NOT EXISTS monthly_reports (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT   NOT NULL,
    ym           CHAR(7)  NOT NULL,
    summary_json JSON     NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_report (user_id, ym),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ───────── 3-B. 돈 계획표 (Numbers '돈계획표' 구조 반영) ─────────
-- 항목(수입/지출/저축)을 한 번 정의하고, 달마다 금액만 넣는 구조.
-- 합계·전달대비·누적·용돈(남은 돈)·달성률은 저장하지 않고 계산한다.
CREATE TABLE IF NOT EXISTS budget_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    kind            VARCHAR(10) NOT NULL,                 -- INCOME, EXPENSE, SAVING
    group_name      VARCHAR(30) NOT NULL,                 -- 고정수입 / 공과금🏠 / 현금💴 ...
    name            VARCHAR(50) NOT NULL,                 -- 会社月給 / 전기료 / 청년도약계좌 ...
    currency        CHAR(3)     NOT NULL DEFAULT 'JPY',   -- JPY, KRW
    opening_balance DECIMAL(15,0) NOT NULL DEFAULT 0,     -- 저축: 시작 전에 이미 모인 돈
    residual        BOOLEAN     NOT NULL DEFAULT FALSE,   -- 용돈: 수입 - 다른 지출 (자동)
    sort_order      INT         NOT NULL DEFAULT 0,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_budget_item (user_id, kind, group_name, name, currency),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS budget_entries (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT        NOT NULL,
    ym      CHAR(7)       NOT NULL,                       -- '2027-04'
    amount  DECIMAL(15,0) NOT NULL DEFAULT 0,
    memo    VARCHAR(300),
    UNIQUE KEY uk_entry (item_id, ym),
    FOREIGN KEY (item_id) REFERENCES budget_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS asset_goals (                  -- 종합표-자산
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    year         INT           NOT NULL,
    name         VARCHAR(50)   NOT NULL,
    saving_group VARCHAR(30)   NOT NULL,                  -- 이 저축 그룹의 누적액이 '현상황'
    currency     CHAR(3)       NOT NULL DEFAULT 'JPY',
    target       DECIMAL(15,0) NOT NULL,
    sort_order   INT           NOT NULL DEFAULT 0,
    UNIQUE KEY uk_asset_goal (user_id, year, name),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quarter_goals (                -- 종합표-분기성장률
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT        NOT NULL,
    year    INT           NOT NULL,
    quarter TINYINT       NOT NULL,
    target  DECIMAL(15,0) NOT NULL,
    UNIQUE KEY uk_quarter (user_id, year, quarter),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS debts (                        -- 셀 메모로 관리하던 빚 상환
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id  BIGINT        NOT NULL,
    name     VARCHAR(50)   NOT NULL,
    total    DECIMAL(15,0) NOT NULL,
    currency CHAR(3)       NOT NULL DEFAULT 'JPY',
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS debt_payments (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    debt_id BIGINT        NOT NULL,
    ym      CHAR(7)       NOT NULL,
    amount  DECIMAL(15,0) NOT NULL,
    UNIQUE KEY uk_debt_pay (debt_id, ym),
    FOREIGN KEY (debt_id) REFERENCES debts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fx_rates (                     -- 환전 기록용 환율 (1엔 = ?원)
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    ym          CHAR(7)       NOT NULL,
    krw_per_jpy DECIMAL(10,4) NOT NULL,
    UNIQUE KEY uk_fx (user_id, ym),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
