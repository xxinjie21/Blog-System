-- =====================================================
-- V4: RBAC 权限表（角色-权限模型）
-- =====================================================

-- 1. 角色表
DROP TABLE IF EXISTS `blog_role`;
CREATE TABLE `blog_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色 ID',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码（唯一标识）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`) COMMENT '角色编码唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 2. 用户-角色关联表
DROP TABLE IF EXISTS `blog_user_role`;
CREATE TABLE `blog_user_role` (
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `role_id` BIGINT NOT NULL COMMENT '角色 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  PRIMARY KEY (`user_id`, `role_id`),
  KEY `idx_role_id` (`role_id`) COMMENT '角色 ID 索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-角色关联表';

-- 3. 权限表
DROP TABLE IF EXISTS `blog_permission`;
CREATE TABLE `blog_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限 ID',
  `name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `code` VARCHAR(50) NOT NULL COMMENT '权限编码（资源:操作）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '权限描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`) COMMENT '权限编码唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 4. 角色-权限关联表
DROP TABLE IF EXISTS `blog_role_permission`;
CREATE TABLE `blog_role_permission` (
  `role_id` BIGINT NOT NULL COMMENT '角色 ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分配时间',
  PRIMARY KEY (`role_id`, `permission_id`),
  KEY `idx_permission_id` (`permission_id`) COMMENT '权限 ID 索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-权限关联表';

-- =====================================================
-- 种子数据
-- =====================================================

-- 角色
INSERT INTO `blog_role` (`name`, `code`, `description`) VALUES
('超级管理员', 'admin', '拥有系统所有权限'),
('编辑', 'editor', '可以管理文章和评论'),
('普通用户', 'user', '可以浏览文章和发表评论');

-- 权限（资源:操作 格式）
INSERT INTO `blog_permission` (`name`, `code`, `description`) VALUES
('文章查看', 'article:read', '查看文章列表和详情'),
('文章创建', 'article:create', '发布新文章'),
('文章编辑', 'article:update', '修改已有文章'),
('文章删除', 'article:delete', '删除文章'),
('分类管理', 'category:manage', '增删改查分类'),
('标签管理', 'tag:manage', '增删改查标签'),
('评论查看', 'comment:read', '查看文章评论'),
('评论创建', 'comment:create', '发表评论'),
('评论删除', 'comment:delete', '删除评论'),
('用户管理', 'user:manage', '管理用户账号');

-- admin 角色：全部权限
INSERT INTO `blog_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM blog_role r, blog_permission p WHERE r.code = 'admin';

-- editor 角色：文章 + 评论 + 分类查看 + 标签查看
INSERT INTO `blog_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM blog_role r, blog_permission p
WHERE r.code = 'editor' AND p.code IN ('article:read','article:create','article:update','article:delete','category:manage','tag:manage','comment:read','comment:create','comment:delete');

-- user 角色：查看 + 评论
INSERT INTO `blog_role_permission` (`role_id`, `permission_id`)
SELECT r.id, p.id FROM blog_role r, blog_permission p
WHERE r.code = 'user' AND p.code IN ('article:read','comment:read','comment:create');

-- admin 用户（id=1）关联 admin 角色
INSERT INTO `blog_user_role` (`user_id`, `role_id`)
SELECT 1, id FROM blog_role WHERE code = 'admin';
