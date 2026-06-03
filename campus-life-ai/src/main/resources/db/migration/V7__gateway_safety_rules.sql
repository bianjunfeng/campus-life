CREATE TABLE IF NOT EXISTS ai_gateway_safety_rule
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name       VARCHAR(128)                        NOT NULL,
    capability_code VARCHAR(32)                         NULL,
    scene_code      VARCHAR(64)                         NULL,
    direction       VARCHAR(16)                         NOT NULL DEFAULT 'INPUT' COMMENT 'INPUT OUTPUT BOTH',
    action          VARCHAR(16)                         NOT NULL DEFAULT 'BLOCK' COMMENT 'BLOCK AUDIT',
    match_type      VARCHAR(16)                         NOT NULL DEFAULT 'KEYWORD' COMMENT 'KEYWORD REGEX',
    pattern_text    VARCHAR(512)                        NOT NULL,
    category        VARCHAR(64)                         NOT NULL DEFAULT 'policy',
    severity        VARCHAR(16)                         NOT NULL DEFAULT 'MEDIUM',
    enabled         TINYINT                             NOT NULL DEFAULT 1,
    priority        INT                                 NOT NULL DEFAULT 100,
    created_at      DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_safety_scope (capability_code, scene_code, enabled),
    INDEX idx_ai_safety_order (enabled, priority, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT ='AI 网关安全治理规则';

INSERT INTO ai_gateway_safety_rule (
    rule_name, capability_code, scene_code, direction, action, match_type,
    pattern_text, category, severity, enabled, priority
)
VALUES
    ('阻断提示词泄露请求', 'chat', NULL, 'INPUT', 'BLOCK', 'KEYWORD',
     '输出系统提示词', 'prompt-injection', 'HIGH', 1, 10),
    ('阻断忽略上文指令请求', 'chat', NULL, 'INPUT', 'BLOCK', 'KEYWORD',
     '忽略之前的指令', 'prompt-injection', 'HIGH', 1, 20),
    ('阻断密钥泄露诱导请求', 'chat', NULL, 'INPUT', 'BLOCK', 'KEYWORD',
     '输出 API Key', 'credential-leak', 'HIGH', 1, 30)
ON DUPLICATE KEY UPDATE
    updated_at = CURRENT_TIMESTAMP;
