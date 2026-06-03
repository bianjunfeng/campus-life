CREATE TABLE IF NOT EXISTS auth_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    access_jti VARCHAR(64) NULL,
    refresh_jti VARCHAR(64) NULL,
    access_token_hash VARCHAR(128) NULL,
    refresh_token_hash VARCHAR(128) NULL,
    device VARCHAR(255) NULL,
    ip VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-在线/有效;0-已退出;2-强制下线;3-已过期',
    login_time DATETIME NOT NULL,
    last_seen_time DATETIME NULL,
    logout_time DATETIME NULL,
    expire_at DATETIME NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_auth_session_session_id (session_id),
    KEY idx_auth_session_user_status_seen (user_id, status, last_seen_time),
    KEY idx_auth_session_status_expire (status, expire_at)
) COMMENT='认证会话表' DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS login_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    account VARCHAR(100) NULL,
    role TINYINT NULL,
    login_type VARCHAR(32) NOT NULL COMMENT 'password/code/wechat/qq',
    success TINYINT NOT NULL,
    failure_reason VARCHAR(255) NULL,
    ip VARCHAR(64) NULL,
    user_agent VARCHAR(512) NULL,
    session_id VARCHAR(64) NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_login_audit_user_time (user_id, create_time),
    KEY idx_login_audit_success_time (success, create_time),
    KEY idx_login_audit_role_time (role, create_time)
) COMMENT='登录审计日志表' DEFAULT CHARSET=utf8mb4;
