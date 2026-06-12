USE dart_service;


CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
    );

CREATE TABLE IF NOT EXISTS users_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    investment_experience VARCHAR(30) NOT NULL,
    risk_tolerance VARCHAR(30) NOT NULL,
    investment_goal VARCHAR(30) NOT NULL,
    investable_amount DECIMAL(15, 0) NOT NULL,
    preferred_sectors VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_profile_user_id (user_id),
    CONSTRAINT fk_users_profile_user
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    );

-- 3. stocks (종목 정보)
CREATE TABLE IF NOT EXISTS stocks (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    stock_code  VARCHAR(10)  NOT NULL,
    corp_code   VARCHAR(8)   NULL,
    stock_name  VARCHAR(100) NOT NULL,
    market      VARCHAR(10)  NOT NULL,
    sector      VARCHAR(50)  NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stocks_code (stock_code),
    INDEX idx_stocks_name (stock_name),
    INDEX idx_stocks_sector (sector)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_info (
    stock_code   VARCHAR(20) NOT NULL,
    sector       VARCHAR(50) NOT NULL,
    dividend_yield DECIMAL(5,2),       -- V1.5
    volatility     DECIMAL(6,3),        -- V1.5
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (stock_code)
    );


-- 4. stock_financials (종목 상세)
CREATE TABLE IF NOT EXISTS stock_financials (
      id                  BIGINT       NOT NULL AUTO_INCREMENT,
      stock_code          VARCHAR(10)  NOT NULL,
      base_year           YEAR         NOT NULL,

-- DART 원자료 (금액)
      revenue             BIGINT,                -- 매출액
      operating_profit    BIGINT,                -- 영업이익
      net_income          BIGINT,                -- 당기순이익
      total_assets        BIGINT,                -- 자산총계
      total_debt          BIGINT,                -- 부채총계
      total_equity        BIGINT,                -- 자본총계 (ROE·부채비율 분모)
      current_assets      BIGINT,                -- 유동자산
      current_liabilities BIGINT,                -- 유동부채
      finance_costs       BIGINT,                -- 금융비용 (이자보상배율 분모)
      operating_cash_flow BIGINT,                -- 영업활동현금흐름

-- 계산된 비율 (저장 시점에 계산)
      debt_ratio          DECIMAL(6, 2),         -- 부채비율 %
      operating_margin    DECIMAL(6, 2),         -- 영업이익률 %
      roe                 DECIMAL(6, 2),         -- ROE %
      current_ratio       DECIMAL(8, 2),         -- 유동비율 %
      interest_coverage   DECIMAL(8, 2),         -- 이자보상배율 (배)

      fetched_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

      PRIMARY KEY (id),
      UNIQUE KEY uk_financials_code_year (stock_code, base_year),
      CONSTRAINT fk_financials_stock FOREIGN KEY (stock_code) REFERENCES stocks (stock_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. stock_reports (종목리포트)
CREATE TABLE IF NOT EXISTS stock_reports (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    stock_code  VARCHAR(10)   NOT NULL,
    content     TEXT          NOT NULL,
    generated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_reports_code_date (stock_code, generated_at),
    INDEX idx_reports_code (stock_code),
    CONSTRAINT fk_reports_stock FOREIGN KEY (stock_code) REFERENCES stocks (stock_code) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. recommendations (추천 종목)
CREATE TABLE IF NOT EXISTS recommendations (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    stock_code   VARCHAR(10)  NOT NULL,
    rec_type     VARCHAR(20)  NOT NULL,   -- 'OVERALL' | 'SECTOR'
    score        DECIMAL(5,2) NOT NULL,
    reason       TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rec_user_stock_type (user_id, stock_code, rec_type),
    INDEX idx_rec_user (user_id),
    CONSTRAINT fk_rec_user  FOREIGN KEY (user_id)    REFERENCES users  (id)         ON DELETE CASCADE,
    CONSTRAINT fk_rec_stock FOREIGN KEY (stock_code) REFERENCES stocks (stock_code) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. holdings (P1 — 나의 투자 현황)
CREATE TABLE IF NOT EXISTS holdings (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    user_id        BIGINT         NOT NULL,
    stock_code     VARCHAR(10)    NOT NULL,
    quantity       INT            NOT NULL,
    purchase_price DECIMAL(12, 2) NOT NULL,
    purchase_date  DATE           NOT NULL,
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_holdings_user (user_id),
    CONSTRAINT fk_holdings_user  FOREIGN KEY (user_id)    REFERENCES users  (id)         ON DELETE CASCADE,
    CONSTRAINT fk_holdings_stock FOREIGN KEY (stock_code) REFERENCES stocks (stock_code) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;