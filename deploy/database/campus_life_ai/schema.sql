-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: campus_life_ai
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ai_call_log`
--

DROP TABLE IF EXISTS `ai_call_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_call_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) NOT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `user_id` bigint unsigned NOT NULL,
  `capability_code` varchar(32) NOT NULL,
  `scene_code` varchar(64) NOT NULL,
  `provider_code` varchar(32) NOT NULL,
  `model_code` varchar(64) DEFAULT NULL,
  `success` tinyint NOT NULL DEFAULT '0',
  `latency_ms` int DEFAULT NULL,
  `prompt_tokens` int DEFAULT NULL,
  `completion_tokens` int DEFAULT NULL,
  `total_tokens` int DEFAULT NULL,
  `fallback_level` int DEFAULT NULL,
  `prompt_chars` int DEFAULT NULL,
  `completion_chars` int DEFAULT NULL,
  `cost_amount` decimal(12,6) DEFAULT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `error_type` varchar(32) DEFAULT NULL,
  `error_message` varchar(512) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_call_log_time` (`created_at`,`id`),
  KEY `idx_ai_call_log_user` (`user_id`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=89 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 调用日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_capability_config`
--

DROP TABLE IF EXISTS `ai_capability_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_capability_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `capability_code` varchar(32) NOT NULL,
  `capability_name` varchar(64) NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT '0',
  `gray_enabled` tinyint NOT NULL DEFAULT '0',
  `gray_rule_json` json DEFAULT NULL,
  `rate_limit_json` json DEFAULT NULL,
  `quota_rule_json` json DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_capability_code` (`capability_code`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 能力配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_conversation`
--

DROP TABLE IF EXISTS `ai_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_conversation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `assistant_type` varchar(32) NOT NULL DEFAULT 'general',
  `capability_code` varchar(32) DEFAULT NULL,
  `scene_code` varchar(64) DEFAULT NULL,
  `provider_code` varchar(32) DEFAULT NULL,
  `model_code` varchar(64) DEFAULT NULL,
  `title` varchar(128) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-正常 1-归档 2-删除',
  `last_message_preview` varchar(500) DEFAULT NULL,
  `last_message_at` datetime DEFAULT NULL,
  `message_count` int NOT NULL DEFAULT '0',
  `pinned` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_conversation_session` (`session_id`),
  KEY `idx_ai_conversation_user_time` (`user_id`,`last_message_at`,`updated_at`),
  KEY `idx_ai_conversation_route` (`capability_code`,`scene_code`,`provider_code`,`model_code`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_gateway_route_rule`
--

DROP TABLE IF EXISTS `ai_gateway_route_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_gateway_route_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `rule_name` varchar(128) NOT NULL,
  `capability_code` varchar(32) NOT NULL,
  `scene_code` varchar(64) DEFAULT NULL,
  `match_rule_json` json DEFAULT NULL,
  `route_rule_json` json NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT '1',
  `priority` int NOT NULL DEFAULT '100',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_route_rule_scene` (`capability_code`,`scene_code`,`enabled`,`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 网关路由规则表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_gateway_safety_rule`
--

DROP TABLE IF EXISTS `ai_gateway_safety_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_gateway_safety_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `rule_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `capability_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scene_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `direction` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'INPUT' COMMENT 'INPUT OUTPUT BOTH',
  `action` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BLOCK' COMMENT 'BLOCK AUDIT',
  `match_type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'KEYWORD' COMMENT 'KEYWORD REGEX',
  `pattern_text` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'policy',
  `severity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MEDIUM',
  `enabled` tinyint NOT NULL DEFAULT '1',
  `priority` int NOT NULL DEFAULT '100',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_safety_scope` (`capability_code`,`scene_code`,`enabled`),
  KEY `idx_ai_safety_order` (`enabled`,`priority`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 网关安全治理规则';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_knowledge_base`
--

DROP TABLE IF EXISTS `ai_knowledge_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_base` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `owner_type` varchar(16) NOT NULL COMMENT 'USER MERCHANT PLATFORM',
  `owner_id` bigint unsigned DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  `visibility` varchar(16) NOT NULL DEFAULT 'PRIVATE' COMMENT 'PRIVATE MERCHANT PUBLIC',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-normal 2-deleted',
  `document_count` int NOT NULL DEFAULT '0',
  `total_size` bigint NOT NULL DEFAULT '0',
  `created_by` bigint unsigned NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_kb_owner` (`owner_type`,`owner_id`,`status`),
  KEY `idx_ai_kb_visibility` (`visibility`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 知识库表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_knowledge_chunk`
--

DROP TABLE IF EXISTS `ai_knowledge_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_chunk` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `kb_id` bigint unsigned NOT NULL,
  `document_id` bigint unsigned NOT NULL,
  `vector_id` varchar(128) NOT NULL,
  `chunk_index` int NOT NULL,
  `content` text NOT NULL,
  `content_hash` varchar(64) NOT NULL,
  `token_count` int DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chunk_vector` (`vector_id`),
  KEY `idx_ai_chunk_doc` (`document_id`,`chunk_index`),
  KEY `idx_ai_chunk_kb` (`kb_id`,`document_id`)
) ENGINE=InnoDB AUTO_INCREMENT=164 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 知识库分片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_knowledge_document`
--

DROP TABLE IF EXISTS `ai_knowledge_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_knowledge_document` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `kb_id` bigint unsigned NOT NULL,
  `title` varchar(255) NOT NULL,
  `original_filename` varchar(255) NOT NULL,
  `file_path` varchar(512) NOT NULL,
  `file_size` bigint NOT NULL DEFAULT '0',
  `content_hash` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-normal 2-deleted',
  `parse_status` tinyint NOT NULL DEFAULT '0' COMMENT '0-pending 1-indexing 2-indexed 3-failed',
  `error_message` varchar(512) DEFAULT NULL,
  `chunk_count` int NOT NULL DEFAULT '0',
  `created_by` bigint unsigned NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_doc_kb` (`kb_id`,`status`,`created_at`),
  KEY `idx_ai_doc_parse` (`parse_status`,`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 知识库文档表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_message`
--

DROP TABLE IF EXISTS `ai_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `role` varchar(16) NOT NULL COMMENT 'user assistant system tool',
  `content_type` varchar(16) NOT NULL DEFAULT 'text',
  `content` longtext NOT NULL,
  `message_status` tinyint NOT NULL DEFAULT '0' COMMENT '0-成功 1-失败 2-生成中 3-取消',
  `provider_code` varchar(32) DEFAULT NULL,
  `model_code` varchar(64) DEFAULT NULL,
  `prompt_tokens` int DEFAULT NULL,
  `completion_tokens` int DEFAULT NULL,
  `total_tokens` int DEFAULT NULL,
  `latency_ms` int DEFAULT NULL,
  `finish_reason` varchar(32) DEFAULT NULL,
  `error_code` varchar(64) DEFAULT NULL,
  `error_message` varchar(512) DEFAULT NULL,
  `reply_to_message_id` bigint unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_message_session_time` (`session_id`,`created_at`,`id`),
  KEY `idx_ai_message_reply` (`reply_to_message_id`)
) ENGINE=InnoDB AUTO_INCREMENT=153 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_model_config`
--

DROP TABLE IF EXISTS `ai_model_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_model_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `provider_code` varchar(32) NOT NULL,
  `model_code` varchar(64) NOT NULL,
  `model_name` varchar(128) NOT NULL,
  `capabilities_json` json DEFAULT NULL,
  `context_window` int DEFAULT NULL,
  `max_output_tokens` int DEFAULT NULL,
  `input_price_per_1k` decimal(10,6) DEFAULT NULL,
  `output_price_per_1k` decimal(10,6) DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT '1',
  `priority` int NOT NULL DEFAULT '100',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_provider_code` (`provider_code`,`model_code`),
  KEY `idx_ai_model_provider_enabled` (`provider_code`,`enabled`,`priority`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 模型配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_provider_config`
--

DROP TABLE IF EXISTS `ai_provider_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_provider_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `provider_code` varchar(32) NOT NULL,
  `provider_name` varchar(64) NOT NULL,
  `base_url` varchar(255) NOT NULL,
  `api_key_cipher` varchar(512) DEFAULT NULL,
  `default_model_code` varchar(64) NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT '0',
  `timeout_ms` int NOT NULL DEFAULT '30000',
  `max_context_messages` int NOT NULL DEFAULT '20',
  `temperature` decimal(4,2) NOT NULL DEFAULT '0.70',
  `top_p` decimal(4,2) DEFAULT NULL,
  `max_output_tokens` int NOT NULL DEFAULT '1024',
  `system_prompt_template` text,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_provider_code` (`provider_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 供应商配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_scene_config`
--

DROP TABLE IF EXISTS `ai_scene_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_scene_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `capability_code` varchar(32) NOT NULL,
  `scene_code` varchar(64) NOT NULL,
  `scene_name` varchar(128) NOT NULL,
  `provider_code` varchar(32) DEFAULT NULL,
  `model_code` varchar(64) DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT '0',
  `system_prompt_template` text,
  `input_schema_json` json DEFAULT NULL,
  `output_schema_json` json DEFAULT NULL,
  `safety_level` varchar(16) DEFAULT NULL,
  `timeout_ms` int DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_scene_code` (`scene_code`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 场景配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ai_usage_quota`
--

DROP TABLE IF EXISTS `ai_usage_quota`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_usage_quota` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `subject_type` varchar(16) NOT NULL COMMENT 'GLOBAL USER ROLE MERCHANT',
  `subject_id` varchar(64) DEFAULT NULL,
  `capability_code` varchar(32) DEFAULT NULL,
  `scene_code` varchar(64) DEFAULT NULL,
  `quota_period` varchar(16) NOT NULL COMMENT 'DAY MONTH',
  `max_calls` int DEFAULT NULL,
  `max_tokens` int DEFAULT NULL,
  `max_cost` decimal(12,4) DEFAULT NULL,
  `enabled` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_quota_subject` (`subject_type`,`subject_id`,`enabled`),
  KEY `idx_ai_quota_scene` (`capability_code`,`scene_code`,`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 使用配额表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'campus_life_ai'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-03 15:25:24
