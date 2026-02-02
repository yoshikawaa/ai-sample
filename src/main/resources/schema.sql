
-- 依存テーブルを先にDROP
DROP TABLE IF EXISTS account_unlock_token;
DROP TABLE IF EXISTS login_attempt;
DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS customer;

CREATE TABLE customer (
    email VARCHAR(255) PRIMARY KEY,
    password VARCHAR(255),
    name VARCHAR(255),
    registration_date DATE,
    birth_date DATE,
    phone_number VARCHAR(20),
    address VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
);

CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    reset_token VARCHAR(255) NOT NULL,
    token_expiry BIGINT NOT NULL,
    FOREIGN KEY (email) REFERENCES customer(email) ON DELETE CASCADE
);

CREATE TABLE login_attempt (
    email VARCHAR(255) PRIMARY KEY,
    attempt_count INT NOT NULL DEFAULT 0,
    locked_until BIGINT,
    last_attempt_time BIGINT NOT NULL
);


-- アカウントアンロック用トークンテーブル
CREATE TABLE account_unlock_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    unlock_token VARCHAR(255) NOT NULL,
    token_expiry BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (email) REFERENCES customer(email) ON DELETE CASCADE
);
