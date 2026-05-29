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
