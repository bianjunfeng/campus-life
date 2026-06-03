UPDATE ai_scene_config
SET enabled = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE scene_code IN ('chat.campus_qa', 'chat.merchant_ops', 'chat.personal_qa', 'chat.mixed_qa');

