-- 系统通知表（Kafka 异步通知落库）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `system_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id` VARCHAR(64) NOT NULL COMMENT '事件唯一ID（幂等）',
  `type` VARCHAR(32) NOT NULL COMMENT '通知类型：LIKE/FAVORITE/COMMENT/MENTION/FOLLOW/LIKE_COMMENT',
  `target_user_id` BIGINT NOT NULL COMMENT '通知接收方用户ID',
  `actor_user_id` BIGINT NOT NULL COMMENT '触发事件用户ID',
  `post_id` BIGINT NULL COMMENT '关联帖子ID',
  `comment_id` BIGINT NULL COMMENT '关联评论ID',
  `message` VARCHAR(255) NULL COMMENT '通知文案',
  `unread` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否未读：1未读 0已读',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_target_time` (`target_user_id`, `create_time`),
  KEY `idx_target_type_time` (`target_user_id`, `type`, `create_time`),
  KEY `idx_actor_time` (`actor_user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

