-- 创建数据库
CREATE DATABASE IF NOT EXISTS file_share DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE file_share;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `open_id` varchar(128) NOT NULL COMMENT '微信OpenID',
  `nickname` varchar(64) DEFAULT NULL COMMENT '用户昵称',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `phone_number` varchar(20) DEFAULT NULL COMMENT '手机号',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态（0-禁用，1-正常）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_open_id` (`open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 文件表
CREATE TABLE IF NOT EXISTS `file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) NOT NULL COMMENT '上传用户ID',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_size` bigint(20) NOT NULL COMMENT '文件大小（字节）',
  `file_type` varchar(128) DEFAULT NULL COMMENT '文件类型',
  `storage_path` varchar(255) NOT NULL COMMENT '存储路径',
  `extract_code` varchar(6) NOT NULL COMMENT '提取码',
  `download_count` int(11) NOT NULL DEFAULT '0' COMMENT '下载次数',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime NOT NULL COMMENT '上传时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '状态（0-已删除，1-正常）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_extract_code` (`extract_code`),
  KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- 下载历史表
CREATE TABLE IF NOT EXISTS `download_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '文件ID',
  `user_id` bigint(20) NOT NULL COMMENT '下载用户ID',
  `download_time` datetime NOT NULL COMMENT '下载时间',
  `download_ip` varchar(64) DEFAULT NULL COMMENT '下载IP',
  PRIMARY KEY (`id`),
  KEY `idx_file_id` (`file_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下载历史表'; 