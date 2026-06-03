CREATE TABLE IF NOT EXISTS ai_usage_quota
(
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    subject_type    VARCHAR(16)                         NOT NULL COMMENT 'GLOBAL USER ROLE MERCHANT',
    subject_id      VARCHAR(64)                         NULL,
    capability_code VARCHAR(32)                         NULL,
    scene_code      VARCHAR(64)                         NULL,
    quota_period    VARCHAR(16)                         NOT NULL COMMENT 'DAY MONTH',
    max_calls       INT                                 NULL,
    max_tokens      INT                                 NULL,
    max_cost        DECIMAL(12, 4)                      NULL,
    enabled         TINYINT                             NOT NULL DEFAULT 1,
    created_at      DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_quota_subject (subject_type, subject_id, enabled),
    INDEX idx_ai_quota_scene (capability_code, scene_code, enabled)
) CHARSET = utf8mb4 COMMENT ='AI 使用配额表';

ALTER TABLE ai_call_log
    ADD COLUMN fallback_level INT NULL AFTER total_tokens,
    ADD COLUMN prompt_chars INT NULL AFTER fallback_level,
    ADD COLUMN completion_chars INT NULL AFTER prompt_chars,
    ADD COLUMN cost_amount DECIMAL(12, 6) NULL AFTER completion_chars,
    ADD COLUMN error_type VARCHAR(32) NULL AFTER error_code;
