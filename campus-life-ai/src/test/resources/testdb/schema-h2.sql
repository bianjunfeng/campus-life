DROP TABLE IF EXISTS ai_call_log;
DROP TABLE IF EXISTS ai_message;
DROP TABLE IF EXISTS ai_conversation;
DROP TABLE IF EXISTS ai_scene_config;
DROP TABLE IF EXISTS ai_capability_config;
DROP TABLE IF EXISTS ai_usage_quota;
DROP TABLE IF EXISTS ai_gateway_safety_rule;
DROP TABLE IF EXISTS ai_gateway_route_rule;
DROP TABLE IF EXISTS ai_model_config;
DROP TABLE IF EXISTS ai_provider_config;
DROP TABLE IF EXISTS ai_knowledge_chunk;
DROP TABLE IF EXISTS ai_knowledge_document;
DROP TABLE IF EXISTS ai_knowledge_base;

CREATE TABLE ai_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    assistant_type VARCHAR(32) NOT NULL DEFAULT 'general',
    capability_code VARCHAR(32),
    scene_code VARCHAR(64),
    provider_code VARCHAR(32),
    model_code VARCHAR(64),
    title VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    last_message_preview VARCHAR(500),
    last_message_at TIMESTAMP NULL,
    message_count INT NOT NULL DEFAULT 0,
    pinned TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_conversation_session UNIQUE (session_id)
);

CREATE TABLE ai_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content_type VARCHAR(16) NOT NULL DEFAULT 'text',
    content CLOB NOT NULL,
    message_status TINYINT NOT NULL DEFAULT 0,
    provider_code VARCHAR(32),
    model_code VARCHAR(64),
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    latency_ms INT,
    finish_reason VARCHAR(32),
    error_code VARCHAR(64),
    error_message VARCHAR(512),
    reply_to_message_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_provider_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_code VARCHAR(32) NOT NULL,
    provider_name VARCHAR(64) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    api_key_cipher VARCHAR(512),
    default_model_code VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 0,
    timeout_ms INT NOT NULL DEFAULT 30000,
    max_context_messages INT NOT NULL DEFAULT 20,
    temperature DECIMAL(4, 2) NOT NULL DEFAULT 0.70,
    top_p DECIMAL(4, 2),
    max_output_tokens INT NOT NULL DEFAULT 1024,
    system_prompt_template CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_provider_code UNIQUE (provider_code)
);

CREATE TABLE ai_model_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_code VARCHAR(32) NOT NULL,
    model_code VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    capabilities_json CLOB,
    context_window INT,
    max_output_tokens INT,
    input_price_per_1k DECIMAL(10, 6),
    output_price_per_1k DECIMAL(10, 6),
    enabled TINYINT NOT NULL DEFAULT 1,
    priority INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_model_provider_code UNIQUE (provider_code, model_code)
);

CREATE TABLE ai_gateway_route_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(128) NOT NULL,
    capability_code VARCHAR(32) NOT NULL,
    scene_code VARCHAR(64),
    match_rule_json CLOB,
    route_rule_json CLOB NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    priority INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_usage_quota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_type VARCHAR(16) NOT NULL,
    subject_id VARCHAR(64),
    capability_code VARCHAR(32),
    scene_code VARCHAR(64),
    quota_period VARCHAR(16) NOT NULL,
    max_calls INT,
    max_tokens INT,
    max_cost DECIMAL(12, 4),
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_gateway_safety_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(128) NOT NULL,
    capability_code VARCHAR(32),
    scene_code VARCHAR(64),
    direction VARCHAR(16) NOT NULL DEFAULT 'INPUT',
    action VARCHAR(16) NOT NULL DEFAULT 'BLOCK',
    match_type VARCHAR(16) NOT NULL DEFAULT 'KEYWORD',
    pattern_text VARCHAR(512) NOT NULL,
    category VARCHAR(64) NOT NULL DEFAULT 'policy',
    severity VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    enabled TINYINT NOT NULL DEFAULT 1,
    priority INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_scene_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    capability_code VARCHAR(32) NOT NULL,
    scene_code VARCHAR(64) NOT NULL,
    scene_name VARCHAR(128) NOT NULL,
    provider_code VARCHAR(32),
    model_code VARCHAR(64),
    enabled TINYINT NOT NULL DEFAULT 0,
    system_prompt_template CLOB,
    input_schema_json CLOB,
    output_schema_json CLOB,
    safety_level VARCHAR(16),
    timeout_ms INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_scene_code UNIQUE (scene_code)
);

CREATE TABLE ai_capability_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    capability_code VARCHAR(32) NOT NULL,
    capability_name VARCHAR(64) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 0,
    gray_enabled TINYINT NOT NULL DEFAULT 0,
    gray_rule_json CLOB,
    rate_limit_json CLOB,
    quota_rule_json CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_capability_code UNIQUE (capability_code)
);

CREATE TABLE ai_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    user_id BIGINT NOT NULL,
    capability_code VARCHAR(32) NOT NULL,
    scene_code VARCHAR(64) NOT NULL,
    provider_code VARCHAR(32) NOT NULL,
    model_code VARCHAR(64),
    success TINYINT NOT NULL DEFAULT 0,
    latency_ms INT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    fallback_level INT,
    prompt_chars INT,
    completion_chars INT,
    cost_amount DECIMAL(12, 6),
    error_code VARCHAR(64),
    error_type VARCHAR(32),
    error_message VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_knowledge_base (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_type VARCHAR(16) NOT NULL,
    owner_id BIGINT,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
    status TINYINT NOT NULL DEFAULT 0,
    document_count INT NOT NULL DEFAULT 0,
    total_size BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    content_hash VARCHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    parse_status TINYINT NOT NULL DEFAULT 0,
    error_message VARCHAR(512),
    chunk_count INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_knowledge_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kb_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    vector_id VARCHAR(128) NOT NULL,
    chunk_index INT NOT NULL,
    content CLOB NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    token_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_chunk_vector UNIQUE (vector_id)
);
