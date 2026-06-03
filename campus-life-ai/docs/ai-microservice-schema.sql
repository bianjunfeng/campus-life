CREATE TABLE IF NOT EXISTS ai_conversation
(
    id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    session_id           VARCHAR(64)                         NOT NULL,
    user_id              BIGINT UNSIGNED                     NOT NULL,
    assistant_type       VARCHAR(32)                         NOT NULL DEFAULT 'general',
    title                VARCHAR(128)                        NOT NULL,
    status               TINYINT                             NOT NULL DEFAULT 0 COMMENT '0-正常 1-归档 2-删除',
    last_message_preview VARCHAR(500)                        NULL,
    last_message_at      DATETIME                            NULL,
    message_count        INT                                 NOT NULL DEFAULT 0,
    pinned               TINYINT                             NOT NULL DEFAULT 0,
    created_at           DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_conversation_session UNIQUE (session_id),
    INDEX idx_ai_conversation_user_time (user_id, last_message_at, updated_at)
) CHARSET = utf8mb4 COMMENT ='AI 会话表';

CREATE TABLE IF NOT EXISTS ai_message
(
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    session_id          VARCHAR(64)                         NOT NULL,
    user_id             BIGINT UNSIGNED                     NOT NULL,
    role                VARCHAR(16)                         NOT NULL COMMENT 'user assistant system tool',
    content_type        VARCHAR(16)                         NOT NULL DEFAULT 'text',
    content             LONGTEXT                            NOT NULL,
    message_status      TINYINT                             NOT NULL DEFAULT 0 COMMENT '0-成功 1-失败 2-生成中 3-取消',
    provider_code       VARCHAR(32)                         NULL,
    model_code          VARCHAR(64)                         NULL,
    prompt_tokens       INT                                 NULL,
    completion_tokens   INT                                 NULL,
    total_tokens        INT                                 NULL,
    latency_ms          INT                                 NULL,
    finish_reason       VARCHAR(32)                         NULL,
    error_code          VARCHAR(64)                         NULL,
    error_message       VARCHAR(512)                        NULL,
    reply_to_message_id BIGINT UNSIGNED                     NULL,
    created_at          DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ai_message_session_time (session_id, created_at, id),
    INDEX idx_ai_message_reply (reply_to_message_id)
) CHARSET = utf8mb4 COMMENT ='AI 消息表';

CREATE TABLE IF NOT EXISTS ai_provider_config
(
    id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    provider_code          VARCHAR(32)                         NOT NULL,
    provider_name          VARCHAR(64)                         NOT NULL,
    base_url               VARCHAR(255)                        NOT NULL,
    api_key_cipher         VARCHAR(512)                        NULL,
    default_model_code     VARCHAR(64)                         NOT NULL,
    enabled                TINYINT                             NOT NULL DEFAULT 0,
    timeout_ms             INT                                 NOT NULL DEFAULT 30000,
    max_context_messages   INT                                 NOT NULL DEFAULT 20,
    temperature            DECIMAL(4, 2)                       NOT NULL DEFAULT 0.70,
    top_p                  DECIMAL(4, 2)                       NULL,
    max_output_tokens      INT                                 NOT NULL DEFAULT 1024,
    system_prompt_template TEXT                                NULL,
    created_at             DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_provider_code UNIQUE (provider_code)
) CHARSET = utf8mb4 COMMENT ='AI 供应商配置表';

CREATE TABLE IF NOT EXISTS ai_scene_config
(
    id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    capability_code        VARCHAR(32)                         NOT NULL,
    scene_code             VARCHAR(64)                         NOT NULL,
    scene_name             VARCHAR(128)                        NOT NULL,
    provider_code          VARCHAR(32)                         NULL,
    model_code             VARCHAR(64)                         NULL,
    enabled                TINYINT                             NOT NULL DEFAULT 0,
    system_prompt_template TEXT                                NULL,
    input_schema_json      JSON                                NULL,
    output_schema_json     JSON                                NULL,
    safety_level           VARCHAR(16)                         NULL,
    timeout_ms             INT                                 NULL,
    created_at             DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_scene_code UNIQUE (scene_code)
) CHARSET = utf8mb4 COMMENT ='AI 场景配置表';

CREATE TABLE IF NOT EXISTS ai_capability_config
(
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    capability_code VARCHAR(32)                         NOT NULL,
    capability_name VARCHAR(64)                         NOT NULL,
    enabled         TINYINT                             NOT NULL DEFAULT 0,
    gray_enabled    TINYINT                             NOT NULL DEFAULT 0,
    gray_rule_json  JSON                                NULL,
    rate_limit_json JSON                                NULL,
    quota_rule_json JSON                                NULL,
    created_at      DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_capability_code UNIQUE (capability_code)
) CHARSET = utf8mb4 COMMENT ='AI 能力配置表';

CREATE TABLE IF NOT EXISTS ai_call_log
(
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    request_id        VARCHAR(64)                         NOT NULL,
    session_id        VARCHAR(64)                         NULL,
    user_id           BIGINT UNSIGNED                     NOT NULL,
    capability_code   VARCHAR(32)                         NOT NULL,
    scene_code        VARCHAR(64)                         NOT NULL,
    provider_code     VARCHAR(32)                         NOT NULL,
    model_code        VARCHAR(64)                         NULL,
    success           TINYINT                             NOT NULL DEFAULT 0,
    latency_ms        INT                                 NULL,
    prompt_tokens     INT                                 NULL,
    completion_tokens INT                                 NULL,
    total_tokens      INT                                 NULL,
    error_code        VARCHAR(64)                         NULL,
    error_message     VARCHAR(512)                        NULL,
    created_at        DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ai_call_log_time (created_at, id),
    INDEX idx_ai_call_log_user (user_id, created_at)
) CHARSET = utf8mb4 COMMENT ='AI 调用日志表';
