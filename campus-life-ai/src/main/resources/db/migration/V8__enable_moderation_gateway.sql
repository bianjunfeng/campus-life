UPDATE ai_capability_config
SET enabled = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE capability_code = 'moderation';

UPDATE ai_scene_config
SET enabled = 1,
    safety_level = 'high',
    system_prompt_template = '你是校园生活平台内容安全审核器。只返回 JSON，不要输出 Markdown。JSON 字段必须包含 result、score、categories、reason。result 只能是 PASS、REJECT、REVIEW；score 是 0 到 1 的风险分；categories 是字符串数组；reason 用中文简述原因。',
    output_schema_json = JSON_OBJECT(
            'type', 'object',
            'required', JSON_ARRAY('result', 'score', 'categories', 'reason'),
            'properties', JSON_OBJECT(
                    'result', JSON_OBJECT('type', 'string', 'enum', JSON_ARRAY('PASS', 'REJECT', 'REVIEW')),
                    'score', JSON_OBJECT('type', 'number', 'minimum', 0, 'maximum', 1),
                    'categories', JSON_OBJECT('type', 'array', 'items', JSON_OBJECT('type', 'string')),
                    'reason', JSON_OBJECT('type', 'string')
                          )
                         ),
    updated_at = CURRENT_TIMESTAMP
WHERE scene_code = 'moderation.text_post';
