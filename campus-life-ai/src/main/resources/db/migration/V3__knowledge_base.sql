CREATE TABLE IF NOT EXISTS ai_knowledge_base
(
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    owner_type     VARCHAR(16)                         NOT NULL COMMENT 'USER MERCHANT PLATFORM',
    owner_id       BIGINT UNSIGNED                     NULL,
    name           VARCHAR(128)                        NOT NULL,
    description    VARCHAR(512)                        NULL,
    visibility     VARCHAR(16)                         NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE MERCHANT PUBLIC',
    status         TINYINT                             NOT NULL DEFAULT 0 COMMENT '0-normal 2-deleted',
    document_count INT                                 NOT NULL DEFAULT 0,
    total_size     BIGINT                              NOT NULL DEFAULT 0,
    created_by     BIGINT UNSIGNED                     NOT NULL,
    created_at     DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_kb_owner (owner_type, owner_id, status),
    INDEX idx_ai_kb_visibility (visibility, status)
) CHARSET = utf8mb4 COMMENT = 'AI 知识库表';

CREATE TABLE IF NOT EXISTS ai_knowledge_document
(
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    kb_id             BIGINT UNSIGNED                     NOT NULL,
    title             VARCHAR(255)                        NOT NULL,
    original_filename VARCHAR(255)                        NOT NULL,
    file_path         VARCHAR(512)                        NOT NULL,
    file_size         BIGINT                              NOT NULL DEFAULT 0,
    content_hash      VARCHAR(64)                         NOT NULL,
    status            TINYINT                             NOT NULL DEFAULT 0 COMMENT '0-normal 2-deleted',
    parse_status      TINYINT                             NOT NULL DEFAULT 0 COMMENT '0-pending 1-indexing 2-indexed 3-failed',
    error_message     VARCHAR(512)                        NULL,
    chunk_count       INT                                 NOT NULL DEFAULT 0,
    created_by        BIGINT UNSIGNED                     NOT NULL,
    created_at        DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_ai_doc_kb (kb_id, status, created_at),
    INDEX idx_ai_doc_parse (parse_status, updated_at)
) CHARSET = utf8mb4 COMMENT = 'AI 知识库文档表';

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk
(
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    kb_id        BIGINT UNSIGNED                     NOT NULL,
    document_id  BIGINT UNSIGNED                     NOT NULL,
    vector_id    VARCHAR(128)                        NOT NULL,
    chunk_index  INT                                 NOT NULL,
    content      TEXT                                NOT NULL,
    content_hash VARCHAR(64)                         NOT NULL,
    token_count  INT                                 NULL,
    created_at   DATETIME                            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_chunk_vector UNIQUE (vector_id),
    INDEX idx_ai_chunk_doc (document_id, chunk_index),
    INDEX idx_ai_chunk_kb (kb_id, document_id)
) CHARSET = utf8mb4 COMMENT = 'AI 知识库分片表';

INSERT INTO ai_scene_config (
    capability_code, scene_code, scene_name, provider_code, model_code, enabled, safety_level, timeout_ms
)
VALUES
    ('chat', 'chat.personal_qa', '个人知识库问答', 'openai-compatible', 'qwen-plus', 1, 'medium', 30000),
    ('chat', 'chat.mixed_qa', '平台与个人知识库问答', 'openai-compatible', 'qwen-plus', 1, 'medium', 30000)
ON DUPLICATE KEY UPDATE
    scene_name = VALUES(scene_name),
    enabled = VALUES(enabled),
    updated_at = CURRENT_TIMESTAMP;
