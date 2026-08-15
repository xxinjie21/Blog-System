-- =====================================================
-- V3: 登录审计日志表（企业级审计要求）
-- 增量迁移：只加一张新表，不动 V1/V2 的任何已执行内容
-- =====================================================

CREATE TABLE `blog_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志 ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户 ID（登录失败时可能为空）',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `operation` VARCHAR(30) NOT NULL COMMENT '操作类型：REGISTER/LOGIN_SUCCESS/LOGIN_FAIL/LOGOUT/REFRESH/CHANGE_PASSWORD',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '客户端 IP',
  `detail` VARCHAR(255) DEFAULT NULL COMMENT '详情（如失败原因）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`) COMMENT '用户 ID 索引 - 按用户审计',
  KEY `idx_operation` (`operation`) COMMENT '操作类型索引 - 按行为审计',
  KEY `idx_create_time` (`create_time`) COMMENT '时间索引 - 按时间审计'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录审计日志表';
