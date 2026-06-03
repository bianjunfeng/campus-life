ALTER TABLE ai_conversation
    ADD COLUMN capability_code VARCHAR(32) NULL AFTER assistant_type,
    ADD COLUMN scene_code VARCHAR(64) NULL AFTER capability_code,
    ADD COLUMN provider_code VARCHAR(32) NULL AFTER scene_code,
    ADD COLUMN model_code VARCHAR(64) NULL AFTER provider_code;

CREATE INDEX idx_ai_conversation_route ON ai_conversation (capability_code, scene_code, provider_code, model_code);
