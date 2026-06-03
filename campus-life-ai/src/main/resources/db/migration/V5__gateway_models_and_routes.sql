CREATE TABLE IF NOT EXISTS ai_model_config
(
    id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    provider_code        VARCHAR(32)                          NOT NULL,
    model_code           VARCHAR(64)                          NOT NULL,
    model_name           VARCHAR(128)                         NOT NULL,
    capabilities_json    JSON                                 NULL,
    context_window       INT                                  NULL,
    max_output_tokens    INT                                  NULL,
    input_price_per_1k   DECIMAL(10, 6)                       NULL,
    output_price_per_1k  DECIMAL(10, 6)                       NULL,
    enabled              TINYINT                              NOT NULL DEFAULT 1,
    priority             INT                                  NOT NULL DEFAULT 100,
    created_at           DATETIME                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME                             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_model_provider_code UNIQUE (provider_code, model_code),
    INDEX idx_ai_model_provider_enabled (provider_code, enabled, priority)
) CHARSET = utf8mb4 COMMENT = 'AI 模型配置表';

CREATE TABLE IF NOT EXISTS ai_gateway_route_rule
(
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    rule_name        VARCHAR(128)                         NOT NULL,
    capability_code  VARCHAR(32)                          NOT NULL,
    scene_code       VARCHAR(64)                          NULL,
    match_rule_json  JSON                                 NULL,
    route_rule_json  JSON                                 NOT NULL,
    enabled          TINYINT                              NOT NULL DEFAULT 1,
    priority         INT                                  NOT NULL DEFAULT 100,
    created_at       DATETIME                             NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME                             NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_route_rule_scene (capability_code, scene_code, enabled, priority)
) CHARSET = utf8mb4 COMMENT = 'AI 网关路由规则表';

INSERT INTO ai_model_config (
    provider_code, model_code, model_name, max_output_tokens, enabled, priority
)
SELECT provider_code, default_model_code, default_model_code, max_output_tokens, enabled, 100
FROM ai_provider_config
ON DUPLICATE KEY UPDATE
    model_name = VALUES(model_name),
    max_output_tokens = VALUES(max_output_tokens),
    updated_at = CURRENT_TIMESTAMP;
