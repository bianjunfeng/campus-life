INSERT INTO ai_capability_config (
    capability_code, capability_name, enabled, gray_enabled, created_at, updated_at
) VALUES (
    'chat', '智能对话', 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ai_provider_config (
    provider_code, provider_name, base_url, api_key_cipher, default_model_code, enabled,
    timeout_ms, max_context_messages, temperature, top_p, max_output_tokens,
    system_prompt_template, created_at, updated_at
) VALUES (
    'integration-test', 'Integration Test Provider', 'https://example.test/v1', NULL, 'integration-default-model', 1,
    30000, 20, 0.70, NULL, 1024,
    'provider system prompt', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ai_scene_config (
    capability_code, scene_code, scene_name, provider_code, model_code, enabled,
    system_prompt_template, safety_level, timeout_ms, created_at, updated_at
) VALUES (
    'chat', 'chat.general', '通用对话', 'integration-test', 'scene-default-model', 1,
    'scene system prompt', 'medium', 30000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ai_scene_config (
    capability_code, scene_code, scene_name, provider_code, model_code, enabled,
    system_prompt_template, safety_level, timeout_ms, created_at, updated_at
) VALUES (
    'chat', 'chat.personal_qa', '个人知识库问答', 'integration-test', 'scene-default-model', 1,
    'personal qa system prompt', 'medium', 30000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
