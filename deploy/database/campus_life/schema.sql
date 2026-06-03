-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: campus_life
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
-- Table structure for table `admin_operation_log`
--

DROP TABLE IF EXISTS `admin_operation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_operation_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `admin_id` bigint unsigned NOT NULL COMMENT '操作管理员ID',
  `operation_type` varchar(50) NOT NULL COMMENT '操作类型：post_manage, merchant_manage, voucher_manage, comment_manage',
  `target_type` varchar(50) NOT NULL COMMENT '目标类型：post, merchant, voucher, comment',
  `target_id` bigint unsigned NOT NULL COMMENT '目标ID',
  `action` varchar(50) NOT NULL COMMENT '操作动作：approve, reject, delete, ban, unpin, pin, set_hot, remove_hot, freeze, unfreeze, etc.',
  `old_status` tinyint DEFAULT NULL COMMENT '操作前状态',
  `new_status` tinyint DEFAULT NULL COMMENT '操作后状态',
  `reason` varchar(500) DEFAULT NULL COMMENT '操作原因/备注',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '操作IP地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_admin_operation_log_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `auth_session`
--

DROP TABLE IF EXISTS `auth_session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `access_jti` varchar(64) DEFAULT NULL,
  `refresh_jti` varchar(64) DEFAULT NULL,
  `access_token_hash` varchar(128) DEFAULT NULL,
  `refresh_token_hash` varchar(128) DEFAULT NULL,
  `device` varchar(255) DEFAULT NULL,
  `ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1-在线/有效;0-已退出;2-强制下线;3-已过期',
  `login_time` datetime NOT NULL,
  `last_seen_time` datetime DEFAULT NULL,
  `logout_time` datetime DEFAULT NULL,
  `expire_at` datetime NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_session_session_id` (`session_id`),
  KEY `idx_auth_session_user_status_seen` (`user_id`,`status`,`last_seen_time`),
  KEY `idx_auth_session_status_expire` (`status`,`expire_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='认证会话表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `browse_history`
--

DROP TABLE IF EXISTS `browse_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `browse_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `post_id` bigint unsigned NOT NULL COMMENT '帖子ID',
  `first_view_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次浏览时间',
  `last_view_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近浏览时间',
  `view_count` int NOT NULL DEFAULT '1' COMMENT '浏览次数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`user_id`,`post_id`),
  KEY `idx_last_view_time` (`last_view_time`),
  KEY `fk_browse_history_post` (`post_id`),
  CONSTRAINT `fk_browse_history_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_browse_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子浏览历史表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comment`
--

DROP TABLE IF EXISTS `comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `post_id` bigint unsigned NOT NULL COMMENT '帖子ID',
  `user_id` bigint unsigned NOT NULL COMMENT '评论用户ID',
  `parent_id` bigint unsigned DEFAULT NULL COMMENT '父级评论ID，一级评论为空',
  `reply_to_user_id` bigint unsigned DEFAULT NULL COMMENT '被回复的用户ID',
  `content` text NOT NULL COMMENT '评论内容',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-正常;1-已删除;2-屏蔽',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_comment_post_status_time` (`post_id`,`status`,`create_time`),
  CONSTRAINT `fk_comment_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2007 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comment_like`
--

DROP TABLE IF EXISTS `comment_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment_like` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `comment_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_user` (`comment_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_comment_like_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comment_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评论点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `event_consume_log`
--

DROP TABLE IF EXISTS `event_consume_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_consume_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `consumer_group` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `topic` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PROCESSING',
  `first_seen_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `processed_at` datetime DEFAULT NULL,
  `last_error` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_consume_event_group` (`event_id`,`consumer_group`),
  KEY `idx_event_consume_status` (`consumer_group`,`status`,`updated_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `event_failure_record`
--

DROP TABLE IF EXISTS `event_failure_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_failure_record` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_topic` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dlt_topic` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `message_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `error` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NEW',
  `replay_count` int NOT NULL DEFAULT '0',
  `failed_at` datetime NOT NULL,
  `last_replay_at` datetime DEFAULT NULL,
  `last_replay_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_replay_message` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_event_failure_channel_time` (`channel`,`failed_at`),
  KEY `idx_event_failure_status` (`status`,`failed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `event_outbox`
--

DROP TABLE IF EXISTS `event_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `event_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `channel` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `topic` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `message_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `event_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `aggregate_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `aggregate_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payload` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NEW',
  `retry_count` int NOT NULL DEFAULT '0',
  `next_retry_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_error` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `sent_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_outbox_event_id` (`event_id`),
  KEY `idx_event_outbox_due` (`status`,`next_retry_at`,`id`),
  KEY `idx_event_outbox_channel` (`channel`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `login_audit_log`
--

DROP TABLE IF EXISTS `login_audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `account` varchar(100) DEFAULT NULL,
  `role` tinyint DEFAULT NULL,
  `login_type` varchar(32) NOT NULL COMMENT 'password/code/wechat/qq',
  `success` tinyint NOT NULL,
  `failure_reason` varchar(255) DEFAULT NULL,
  `ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(512) DEFAULT NULL,
  `session_id` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_login_audit_user_time` (`user_id`,`create_time`),
  KEY `idx_login_audit_success_time` (`success`,`create_time`),
  KEY `idx_login_audit_role_time` (`role`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录审计日志表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `login_token`
--

DROP TABLE IF EXISTS `login_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `login_token` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `token` varchar(128) NOT NULL COMMENT '登录凭证',
  `device` varchar(100) DEFAULT NULL COMMENT '设备信息',
  `ip` varchar(50) DEFAULT NULL COMMENT '登录IP',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1-有效;0-已失效',
  `expire_at` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_expire_at` (`expire_at`),
  CONSTRAINT `fk_login_token_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录 Token 表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchant`
--

DROP TABLE IF EXISTS `merchant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '关联用户ID（商家账号）',
  `name` varchar(100) NOT NULL COMMENT '商家名称',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人姓名',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `business_hours` varchar(80) DEFAULT NULL COMMENT '营业时间',
  `longitude` decimal(10,6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,6) DEFAULT NULL COMMENT '纬度',
  `geo_hash` varchar(32) DEFAULT NULL COMMENT 'GeoHash编码',
  `province` varchar(50) DEFAULT NULL COMMENT '省',
  `city` varchar(50) DEFAULT NULL COMMENT '市',
  `district` varchar(50) DEFAULT NULL COMMENT '区县',
  `location_status` tinyint NOT NULL DEFAULT '0' COMMENT '定位状态：0未设置 1已设置',
  `location_updated_time` datetime DEFAULT NULL COMMENT '定位更新时间',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待审核;1-正常;2-冻结;3-关闭',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `type_id` bigint DEFAULT NULL COMMENT '商家类型ID，关联 merchant_type(id)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_user` (`user_id`),
  KEY `idx_type_id` (`type_id`),
  KEY `idx_status` (`status`),
  KEY `idx_status_type` (`status`,`type_id`),
  KEY `idx_lng_lat` (`longitude`,`latitude`),
  CONSTRAINT `fk_merchant_type` FOREIGN KEY (`type_id`) REFERENCES `merchant_type` (`id`),
  CONSTRAINT `fk_merchant_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchant_auth_request`
--

DROP TABLE IF EXISTS `merchant_auth_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant_auth_request` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '申请用户ID',
  `merchant_name` varchar(100) NOT NULL COMMENT '商家名称',
  `license_no` varchar(100) DEFAULT NULL COMMENT '营业执照号/统一社会信用代码',
  `license_img` varchar(255) DEFAULT NULL COMMENT '证照图片URL',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待审核;1-通过;2-驳回',
  `reason` varchar(255) DEFAULT NULL COMMENT '驳回原因',
  `reviewer_id` bigint unsigned DEFAULT NULL COMMENT '审核管理员ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `review_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_merchant_auth_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家认证申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchant_type`
--

DROP TABLE IF EXISTS `merchant_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchant_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` varchar(50) NOT NULL COMMENT '类型编码，如 drink, food, study',
  `name` varchar(50) NOT NULL COMMENT '类型名称，如 奶茶饮品、正餐餐饮',
  `description` varchar(255) DEFAULT NULL COMMENT '类型说明',
  `sort_order` int DEFAULT '0' COMMENT '排序优先级，越大越靠前',
  `status` tinyint DEFAULT '1' COMMENT '状态：1-启用 0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `message`
--

DROP TABLE IF EXISTS `message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `conversation_id` varchar(64) NOT NULL COMMENT '会话ID，可按小ID_大ID生成',
  `from_user_id` bigint unsigned NOT NULL COMMENT '发送者',
  `to_user_id` bigint unsigned NOT NULL COMMENT '接收者',
  `content` text NOT NULL COMMENT '内容',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-未读;1-已读;2-撤回/删除',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_to_user_id` (`to_user_id`),
  KEY `idx_message_conversation_status_time` (`conversation_id`,`status`,`create_time`,`id`),
  KEY `idx_message_to_status_conversation` (`to_user_id`,`status`,`conversation_id`,`create_time`),
  KEY `idx_message_from_status_conversation` (`from_user_id`,`status`,`conversation_id`,`create_time`),
  CONSTRAINT `fk_message_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_message_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2005 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私信消息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_callback_log`
--

DROP TABLE IF EXISTS `payment_callback_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_callback_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `channel` varchar(20) NOT NULL COMMENT '渠道',
  `callback_type` varchar(32) NOT NULL COMMENT '回调类型: PAY_NOTIFY/REFUND_NOTIFY',
  `payment_no` varchar(64) DEFAULT NULL COMMENT '支付单号',
  `biz_order_no` varchar(64) DEFAULT NULL COMMENT '业务订单号',
  `raw_body` text NOT NULL COMMENT '原始回调内容',
  `signature_verified` tinyint NOT NULL DEFAULT '0' COMMENT '是否验签通过: 0否 1是',
  `amount_verified` tinyint NOT NULL DEFAULT '0' COMMENT '是否金额校验通过: 0否 1是',
  `process_status` varchar(24) NOT NULL COMMENT '处理状态: RECEIVED/PROCESSED/FAILED/IGNORED',
  `error_message` varchar(255) DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payment_callback_payment_no` (`payment_no`),
  KEY `idx_payment_callback_channel_time` (`channel`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付回调日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_order`
--

DROP TABLE IF EXISTS `payment_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `payment_no` varchar(64) NOT NULL COMMENT '支付单号',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型: VOUCHER/GOODS/MEMBER/RECHARGE',
  `biz_order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `channel` varchar(20) NOT NULL COMMENT '支付渠道: alipay/wechat/wallet',
  `title` varchar(128) DEFAULT NULL COMMENT '支付标题',
  `description` varchar(255) DEFAULT NULL COMMENT '支付描述',
  `amount` decimal(12,2) NOT NULL COMMENT '支付金额',
  `refunded_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '累计退款金额',
  `status` varchar(24) NOT NULL COMMENT '支付状态: INIT/WAIT_PAY/SUCCESS/FAILED/CLOSED/PARTIAL_REFUNDED/FULL_REFUNDED',
  `channel_trade_no` varchar(128) DEFAULT NULL COMMENT '渠道交易号',
  `idempotency_key` varchar(80) DEFAULT NULL COMMENT '创建支付幂等键',
  `expire_time` datetime DEFAULT NULL COMMENT '支付过期时间',
  `success_time` datetime DEFAULT NULL COMMENT '支付成功时间',
  `last_callback_time` datetime DEFAULT NULL COMMENT '最近一次回调时间',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_order_payment_no` (`payment_no`),
  UNIQUE KEY `uk_payment_order_idempotency_key` (`idempotency_key`),
  KEY `idx_payment_order_biz` (`biz_type`,`biz_order_no`),
  KEY `idx_payment_order_user_time` (`user_id`,`create_time`),
  KEY `idx_payment_order_channel_status` (`channel`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_reconciliation_issue`
--

DROP TABLE IF EXISTS `payment_reconciliation_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_reconciliation_issue` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `issue_key` varchar(128) NOT NULL COMMENT '差错唯一键',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型',
  `payment_no` varchar(64) DEFAULT NULL COMMENT '支付单号',
  `biz_order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `user_id` bigint unsigned DEFAULT NULL COMMENT '用户ID',
  `channel` varchar(20) DEFAULT NULL COMMENT '支付渠道',
  `issue_type` varchar(64) NOT NULL COMMENT '差错类型',
  `issue_level` varchar(16) NOT NULL DEFAULT 'WARN' COMMENT '差错级别: INFO/WARN/CRITICAL',
  `issue_status` varchar(16) NOT NULL DEFAULT 'OPEN' COMMENT '差错状态: OPEN/RESOLVED',
  `issue_message` varchar(255) NOT NULL COMMENT '差错描述',
  `payment_order_status` varchar(32) DEFAULT NULL COMMENT '支付单状态',
  `voucher_order_status` int DEFAULT NULL COMMENT '券订单状态',
  `voucher_payment_status` int DEFAULT NULL COMMENT '券订单支付状态',
  `payment_amount` decimal(12,2) DEFAULT NULL COMMENT '支付金额',
  `recorded_refunded_amount` decimal(12,2) DEFAULT NULL COMMENT '支付单记录退款金额',
  `actual_refunded_amount` decimal(12,2) DEFAULT NULL COMMENT '成功退款累计金额',
  `last_checked_time` datetime NOT NULL COMMENT '最后扫描时间',
  `resolved_time` datetime DEFAULT NULL COMMENT '处理完成时间',
  `resolved_by` bigint unsigned DEFAULT NULL COMMENT '处理人ID',
  `resolve_note` varchar(255) DEFAULT NULL COMMENT '处理备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_reconciliation_issue_key` (`issue_key`),
  KEY `idx_payment_reconciliation_status_type` (`issue_status`,`issue_type`),
  KEY `idx_payment_reconciliation_biz_order_no` (`biz_order_no`),
  KEY `idx_payment_reconciliation_payment_no` (`payment_no`)
) ENGINE=InnoDB AUTO_INCREMENT=199 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付对账差错单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_refund_order`
--

DROP TABLE IF EXISTS `payment_refund_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_refund_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `refund_no` varchar(64) NOT NULL COMMENT '退款单号',
  `payment_no` varchar(64) NOT NULL COMMENT '支付单号',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型',
  `biz_order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `merchant_id` bigint unsigned DEFAULT NULL COMMENT '所属商家ID',
  `channel` varchar(20) NOT NULL COMMENT '退款渠道',
  `refund_amount` decimal(12,2) NOT NULL COMMENT '退款金额',
  `requested_amount` decimal(12,2) DEFAULT NULL COMMENT '申请退款金额',
  `approved_amount` decimal(12,2) DEFAULT NULL COMMENT '审核通过金额',
  `reason` varchar(255) DEFAULT NULL COMMENT '退款原因',
  `status` varchar(24) NOT NULL COMMENT '退款状态: INIT/SUBMITTED/SUCCESS/FAILED/CLOSED',
  `current_reviewer_role` varchar(20) DEFAULT NULL COMMENT '当前审核角色: MERCHANT/ADMIN/SYSTEM',
  `current_reviewer_id` bigint unsigned DEFAULT NULL COMMENT '当前审核人ID',
  `review_deadline` datetime DEFAULT NULL COMMENT '当前审核截止时间',
  `merchant_review_reason` varchar(255) DEFAULT NULL COMMENT '商家审核说明',
  `merchant_review_time` datetime DEFAULT NULL COMMENT '商家审核时间',
  `merchant_reviewer_id` bigint unsigned DEFAULT NULL COMMENT '商家审核人ID',
  `admin_review_reason` varchar(255) DEFAULT NULL COMMENT '管理员审核说明',
  `admin_review_time` datetime DEFAULT NULL COMMENT '管理员审核时间',
  `admin_reviewer_id` bigint unsigned DEFAULT NULL COMMENT '管理员审核人ID',
  `channel_refund_no` varchar(128) DEFAULT NULL COMMENT '渠道退款单号',
  `request_idempotency_key` varchar(80) DEFAULT NULL COMMENT '退款请求幂等键',
  `success_time` datetime DEFAULT NULL COMMENT '退款成功时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_refund_order_refund_no` (`refund_no`),
  UNIQUE KEY `uk_payment_refund_order_request_idempotency_key` (`request_idempotency_key`),
  KEY `idx_payment_refund_payment_no` (`payment_no`),
  KEY `idx_payment_refund_biz` (`biz_type`,`biz_order_no`),
  KEY `idx_payment_refund_merchant_status` (`merchant_id`,`status`),
  KEY `idx_payment_refund_user_status` (`user_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付退款单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `payment_refund_review_log`
--

DROP TABLE IF EXISTS `payment_refund_review_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `payment_refund_review_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `refund_no` varchar(64) NOT NULL COMMENT '退款单号',
  `operator_role` varchar(20) NOT NULL COMMENT '操作角色: USER/MERCHANT/ADMIN/SYSTEM',
  `operator_id` bigint unsigned DEFAULT NULL COMMENT '操作人ID',
  `action` varchar(32) NOT NULL COMMENT '动作',
  `from_status` varchar(32) DEFAULT NULL COMMENT '原状态',
  `to_status` varchar(32) DEFAULT NULL COMMENT '目标状态',
  `comment` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payment_refund_review_log_refund_no` (`refund_no`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='退款审核日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post`
--

DROP TABLE IF EXISTS `post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '作者ID',
  `category_id` bigint unsigned DEFAULT NULL COMMENT '分类ID，允许为空表示未分类',
  `title` varchar(255) DEFAULT NULL COMMENT '标题，可为空（纯想法类）',
  `content` text NOT NULL COMMENT '正文内容',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-正常;1-仅自己可见;2-已删除;3-屏蔽',
  `is_pinned` tinyint NOT NULL DEFAULT '0' COMMENT '是否置顶',
  `is_hot` tinyint NOT NULL DEFAULT '0' COMMENT '是否热门',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `comment_count` int NOT NULL DEFAULT '0' COMMENT '评论数',
  `favorite_count` int NOT NULL DEFAULT '0' COMMENT '收藏数',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '浏览次数',
  `last_comment_time` datetime DEFAULT NULL COMMENT '最后评论时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_hot` (`is_hot`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_last_comment_time` (`last_comment_time`),
  KEY `idx_post_status_create_time` (`status`,`create_time` DESC),
  KEY `idx_post_category_status_pin_time` (`category_id`,`status`,`is_pinned`,`create_time` DESC),
  KEY `idx_post_user_status_time` (`user_id`,`status`,`create_time` DESC),
  CONSTRAINT `fk_post_category` FOREIGN KEY (`category_id`) REFERENCES `post_category` (`id`),
  CONSTRAINT `fk_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=155 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_category`
--

DROP TABLE IF EXISTS `post_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_category` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '分类名称，如：树洞、学习、二手、活动',
  `code` varchar(50) NOT NULL COMMENT '英文编码，便于前端路由或内部使用',
  `description` varchar(255) DEFAULT NULL COMMENT '分类描述',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序，越大越靠前',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1-启用;0-停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_favorite`
--

DROP TABLE IF EXISTS `post_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_favorite` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `post_id` bigint unsigned NOT NULL COMMENT '帖子ID',
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_post_favorite_user_time_post` (`user_id`,`create_time` DESC,`post_id`),
  CONSTRAINT `fk_post_favorite_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_post_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_image`
--

DROP TABLE IF EXISTS `post_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_image` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `post_id` bigint unsigned NOT NULL COMMENT '帖子ID',
  `url` longtext NOT NULL COMMENT '图片地址（支持 base64 或 URL）',
  `width` int DEFAULT NULL COMMENT '原始宽度',
  `height` int DEFAULT NULL COMMENT '原始高度',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序，越大越靠前/越后',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_post_image_post_sort_id` (`post_id`,`sort_order` DESC,`id`),
  CONSTRAINT `fk_post_image_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=155 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子图片表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_like`
--

DROP TABLE IF EXISTS `post_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_like` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `post_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`,`user_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_post_like_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_post_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='帖子点赞表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `report`
--

DROP TABLE IF EXISTS `report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `report` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint unsigned NOT NULL COMMENT '举报人ID',
  `target_type` tinyint NOT NULL COMMENT '1-帖子;2-评论;3-用户',
  `target_id` bigint unsigned NOT NULL COMMENT '目标ID',
  `reason` varchar(255) NOT NULL COMMENT '举报原因',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待处理;1-已处理;2-忽略',
  `handler_id` bigint unsigned DEFAULT NULL COMMENT '处理人（管理员）',
  `handle_result` varchar(255) DEFAULT NULL COMMENT '处理结果说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_target` (`target_type`,`target_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_report_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='举报表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `search_sync_checkpoint`
--

DROP TABLE IF EXISTS `search_sync_checkpoint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_sync_checkpoint` (
  `checkpoint_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `checkpoint_millis` bigint NOT NULL,
  `last_success_at` datetime DEFAULT NULL,
  `last_error` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`checkpoint_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `seckill_voucher`
--

DROP TABLE IF EXISTS `seckill_voucher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `seckill_voucher` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `voucher_id` bigint unsigned NOT NULL COMMENT '关联 voucher',
  `stock` int NOT NULL DEFAULT '0' COMMENT '秒杀库存',
  `start_time` datetime NOT NULL COMMENT '秒杀开始时间',
  `end_time` datetime NOT NULL COMMENT '秒杀结束时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_voucher_id` (`voucher_id`),
  CONSTRAINT `fk_seckill_voucher_voucher` FOREIGN KEY (`voucher_id`) REFERENCES `voucher` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='秒杀券表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shopping_cart`
--

DROP TABLE IF EXISTS `shopping_cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shopping_cart` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `voucher_id` bigint unsigned NOT NULL COMMENT '优惠券ID',
  `quantity` int NOT NULL DEFAULT '1' COMMENT '购买数量',
  `selected` tinyint NOT NULL DEFAULT '1' COMMENT '是否选中: 1是 0否',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cart_user_voucher` (`user_id`,`voucher_id`),
  KEY `fk_cart_voucher` (`voucher_id`),
  KEY `idx_cart_user_update_time` (`user_id`,`update_time`),
  CONSTRAINT `fk_cart_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_voucher` FOREIGN KEY (`voucher_id`) REFERENCES `voucher` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student_auth_request`
--

DROP TABLE IF EXISTS `student_auth_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_auth_request` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '申请人ID',
  `real_name` varchar(50) NOT NULL,
  `school` varchar(100) NOT NULL,
  `college` varchar(100) DEFAULT NULL,
  `major` varchar(100) DEFAULT NULL,
  `grade` varchar(20) DEFAULT NULL,
  `class_name` varchar(50) DEFAULT NULL,
  `student_no` varchar(50) NOT NULL,
  `student_card_img` varchar(255) DEFAULT NULL COMMENT '学生证照片URL',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待审核;1-通过;2-驳回',
  `reason` varchar(255) DEFAULT NULL COMMENT '驳回原因',
  `reviewer_id` bigint unsigned DEFAULT NULL COMMENT '审核管理员ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `review_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_student_auth_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生认证申请表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student_profile`
--

DROP TABLE IF EXISTS `student_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student_profile` (
  `user_id` bigint unsigned NOT NULL COMMENT '关联用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `school` varchar(100) NOT NULL COMMENT '学校',
  `college` varchar(100) DEFAULT NULL COMMENT '学院',
  `major` varchar(100) DEFAULT NULL COMMENT '专业',
  `grade` varchar(20) DEFAULT NULL COMMENT '年级，如2022',
  `class_name` varchar(50) DEFAULT NULL COMMENT '班级',
  `student_no` varchar(50) NOT NULL COMMENT '学号',
  `occupation` varchar(50) DEFAULT NULL COMMENT '职业（学生相关，如：在校学生、研究生等）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待审核;1-已通过/有效;2-已拒绝/无效',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_student_no` (`school`,`student_no`),
  CONSTRAINT `fk_student_profile_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生档案表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_notification`
--

DROP TABLE IF EXISTS `system_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件唯一ID（幂等）',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知类型：LIKE/FAVORITE/COMMENT/MENTION/FOLLOW/LIKE_COMMENT',
  `target_user_id` bigint NOT NULL COMMENT '通知接收方用户ID',
  `actor_user_id` bigint NOT NULL COMMENT '触发事件用户ID',
  `post_id` bigint DEFAULT NULL COMMENT '关联帖子ID',
  `comment_id` bigint DEFAULT NULL COMMENT '关联评论ID',
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '通知文案',
  `unread` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否未读：1未读 0已读',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_id` (`event_id`),
  KEY `idx_target_time` (`target_user_id`,`create_time`),
  KEY `idx_target_type_time` (`target_user_id`,`type`,`create_time`),
  KEY `idx_actor_time` (`actor_user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `wechat_openid` varchar(100) DEFAULT NULL COMMENT '微信OpenID',
  `qq_openid` varchar(100) DEFAULT NULL COMMENT 'QQ OpenID',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `username` varchar(50) NOT NULL COMMENT '昵称/用户名',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希',
  `salt` varchar(64) DEFAULT NULL COMMENT '密码盐',
  `gender` tinyint NOT NULL DEFAULT '0' COMMENT '性别：0-未知/保密; 1-男; 2-女',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `region` varchar(100) DEFAULT NULL COMMENT '地区/城市',
  `role` tinyint NOT NULL DEFAULT '0' COMMENT '0-学生;1-商家;2-管理员',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0-禁用;1-正常',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像地址',
  `bio` varchar(255) DEFAULT NULL COMMENT '个人简介',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  UNIQUE KEY `uk_email` (`email`),
  UNIQUE KEY `uk_wechat_openid` (`wechat_openid`),
  UNIQUE KEY `uk_qq_openid` (`qq_openid`),
  KEY `idx_role` (`role`),
  KEY `idx_status` (`status`),
  KEY `idx_user_status_create_time` (`status`,`create_time` DESC)
) ENGINE=InnoDB AUTO_INCREMENT=18573 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_follow`
--

DROP TABLE IF EXISTS `user_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_follow` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `follower_id` bigint unsigned NOT NULL COMMENT '关注发起者',
  `followee_id` bigint unsigned NOT NULL COMMENT '被关注的人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_follow_pair` (`follower_id`,`followee_id`),
  KEY `idx_followee_id` (`followee_id`),
  KEY `idx_user_follow_follower_time` (`follower_id`,`create_time` DESC,`followee_id`),
  KEY `idx_user_follow_followee_time` (`followee_id`,`create_time` DESC,`follower_id`),
  CONSTRAINT `fk_user_follow_followee` FOREIGN KEY (`followee_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_user_follow_follower` FOREIGN KEY (`follower_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_wallet`
--

DROP TABLE IF EXISTS `user_wallet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_wallet` (
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `balance` decimal(12,2) NOT NULL DEFAULT '1000.00' COMMENT '可用余额',
  `frozen_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '冻结金额',
  `total_recharge` decimal(12,2) NOT NULL DEFAULT '1000.00' COMMENT '累计入账',
  `total_spent` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '累计消费',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1-正常;0-冻结',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  CONSTRAINT `fk_user_wallet_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户钱包表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `voucher`
--

DROP TABLE IF EXISTS `voucher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `merchant_id` bigint unsigned NOT NULL COMMENT '商家ID',
  `title` varchar(100) NOT NULL COMMENT '券标题',
  `sub_title` varchar(255) DEFAULT NULL COMMENT '副标题/描述',
  `image_url` varchar(255) DEFAULT NULL,
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存',
  `sold_count` int NOT NULL DEFAULT '0' COMMENT '已售数量（支付成功）',
  `amount` decimal(10,2) NOT NULL COMMENT '面值/优惠金额',
  `pay_value` decimal(10,2) DEFAULT NULL COMMENT '需要支付的金额（如团购券）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1-上架;0-下架',
  `begin_time` datetime DEFAULT NULL COMMENT '可使用起始时间',
  `end_time` datetime DEFAULT NULL COMMENT '可使用结束时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_voucher_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券基础表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `voucher_order`
--

DROP TABLE IF EXISTS `voucher_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `voucher_order` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `voucher_id` bigint unsigned NOT NULL COMMENT '券ID',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待使用;1-已使用;2-已过期;3-已退款',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `pay_amount` decimal(10,2) DEFAULT NULL COMMENT '支付金额',
  `payment_method` varchar(20) DEFAULT NULL COMMENT '支付方式：alipay/wechat',
  `payment_idempotency_key` varchar(80) DEFAULT NULL COMMENT '支付请求幂等键',
  `payment_status` tinyint NOT NULL DEFAULT '0' COMMENT '0-待支付;1-支付成功;2-支付失败;3-已退款',
  `order_source` varchar(20) NOT NULL DEFAULT 'app' COMMENT '订单来源：app/web/admin/seckill',
  `pay_deadline` datetime DEFAULT NULL COMMENT '支付截止时间',
  `use_deadline` datetime DEFAULT NULL COMMENT '使用截止时间快照',
  `cancel_reason` varchar(255) DEFAULT NULL COMMENT '取消原因',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_voucher_order_payment_idempotency_key` (`payment_idempotency_key`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_voucher_id` (`voucher_id`),
  KEY `idx_status` (`status`),
  KEY `idx_user_status_time` (`user_id`,`status`,`create_time`),
  KEY `idx_voucher_order_use_deadline_refund` (`status`,`payment_status`,`use_deadline`),
  KEY `idx_voucher_order_status_pay_deadline` (`status`,`pay_deadline`),
  KEY `idx_voucher_order_status_payment_use_deadline` (`status`,`payment_status`,`use_deadline`),
  CONSTRAINT `fk_voucher_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_voucher_order_voucher` FOREIGN KEY (`voucher_id`) REFERENCES `voucher` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24842 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wallet_transaction`
--

DROP TABLE IF EXISTS `wallet_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallet_transaction` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL COMMENT '用户ID',
  `biz_type` tinyint NOT NULL COMMENT '1-充值;2-消费;3-退款;4-调整',
  `amount` decimal(12,2) NOT NULL COMMENT '变动金额（正数）',
  `balance_after` decimal(12,2) NOT NULL COMMENT '变动后余额',
  `order_no` varchar(64) DEFAULT NULL COMMENT '关联订单号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_wallet_tx_user_time` (`user_id`,`create_time`),
  KEY `idx_wallet_tx_order_no` (`order_no`),
  CONSTRAINT `fk_wallet_transaction_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='钱包交易流水';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'campus_life'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-03 15:25:23
