SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `event_outbox` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `event_id` VARCHAR(64) NOT NULL,
  `channel` VARCHAR(64) NOT NULL,
  `topic` VARCHAR(128) NOT NULL,
  `message_key` VARCHAR(128) NULL,
  `event_type` VARCHAR(64) NOT NULL,
  `aggregate_type` VARCHAR(64) NULL,
  `aggregate_id` VARCHAR(128) NULL,
  `payload` LONGTEXT NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'NEW',
  `retry_count` INT NOT NULL DEFAULT 0,
  `next_retry_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_error` VARCHAR(1024) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `sent_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_outbox_event_id` (`event_id`),
  KEY `idx_event_outbox_due` (`status`, `next_retry_at`, `id`),
  KEY `idx_event_outbox_channel` (`channel`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Kafka outbox event table';

CREATE TABLE IF NOT EXISTS `event_consume_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `event_id` VARCHAR(64) NOT NULL,
  `consumer_group` VARCHAR(128) NOT NULL,
  `topic` VARCHAR(128) NULL,
  `message_key` VARCHAR(128) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PROCESSING',
  `first_seen_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `processed_at` DATETIME NULL,
  `last_error` VARCHAR(1024) NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_consume_event_group` (`event_id`, `consumer_group`),
  KEY `idx_event_consume_status` (`consumer_group`, `status`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Kafka consumer idempotency log';

CREATE TABLE IF NOT EXISTS `event_failure_record` (
  `id` VARCHAR(64) NOT NULL,
  `channel` VARCHAR(64) NOT NULL,
  `original_topic` VARCHAR(128) NULL,
  `dlt_topic` VARCHAR(128) NULL,
  `message_key` VARCHAR(128) NULL,
  `payload` LONGTEXT NOT NULL,
  `error` VARCHAR(1024) NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'NEW',
  `replay_count` INT NOT NULL DEFAULT 0,
  `failed_at` DATETIME NOT NULL,
  `last_replay_at` DATETIME NULL,
  `last_replay_status` VARCHAR(16) NULL,
  `last_replay_message` VARCHAR(1024) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_event_failure_channel_time` (`channel`, `failed_at`),
  KEY `idx_event_failure_status` (`status`, `failed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Kafka DLT and failure records';

CREATE TABLE IF NOT EXISTS `search_sync_checkpoint` (
  `checkpoint_name` VARCHAR(64) NOT NULL,
  `checkpoint_millis` BIGINT NOT NULL,
  `last_success_at` DATETIME NULL,
  `last_error` VARCHAR(1024) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`checkpoint_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Persistent search compensation checkpoint';
