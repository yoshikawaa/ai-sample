
-- 依存テーブルを先にDROP
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS login_history;
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

-- ログイン履歴テーブル
CREATE TABLE login_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,  -- 'SUCCESS', 'FAILURE', 'LOCKED', 'LOGOUT'
    ip_address VARCHAR(45),       -- IPv4/IPv6対応
    user_agent TEXT,
    failure_reason VARCHAR(100),  -- 失敗理由（パスワード誤り、アカウントロック等）
    FOREIGN KEY (email) REFERENCES customer(email) ON DELETE CASCADE
);

CREATE INDEX idx_login_history_email ON login_history(email);
CREATE INDEX idx_login_history_login_time ON login_history(login_time);
CREATE INDEX idx_login_history_status ON login_history(status);

-- 監査ログテーブル
CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    performed_by VARCHAR(255) NOT NULL,  -- 操作者のメールアドレス
    target_email VARCHAR(255),           -- 対象顧客のメールアドレス
    action_type VARCHAR(50) NOT NULL,    -- 'CREATE', 'UPDATE', 'DELETE', 'PASSWORD_RESET', 'ACCOUNT_LOCK', 'ACCOUNT_UNLOCK'
    action_detail TEXT,                  -- 変更内容の詳細（JSON形式）
    action_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    FOREIGN KEY (performed_by) REFERENCES customer(email) ON DELETE CASCADE,
    FOREIGN KEY (target_email) REFERENCES customer(email) ON DELETE CASCADE
);

CREATE INDEX idx_audit_log_performed_by ON audit_log(performed_by);
CREATE INDEX idx_audit_log_target_email ON audit_log(target_email);
CREATE INDEX idx_audit_log_action_type ON audit_log(action_type);
CREATE INDEX idx_audit_log_action_time ON audit_log(action_time);
