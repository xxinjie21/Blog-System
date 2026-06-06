-- =====================================================
-- 高性能缓存博客系统 - 数据库初始化脚本
-- =====================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `blog_system` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `blog_system`;

-- =====================================================
-- 1. 文章表 blog_article
-- =====================================================
DROP TABLE IF EXISTS `blog_article`;
CREATE TABLE `blog_article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章 ID',
  `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
  `content` LONGTEXT NOT NULL COMMENT '文章内容',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图片 URL',
  `category_id` BIGINT NOT NULL COMMENT '分类 ID',
  `author` VARCHAR(50) NOT NULL COMMENT '作者',
  `view_count` INT DEFAULT '0' COMMENT '浏览量',
  `like_count` INT DEFAULT '0' COMMENT '点赞数',
  `comment_count` INT DEFAULT '0' COMMENT '评论数',
  `is_top` TINYINT DEFAULT '0' COMMENT '是否置顶 0-否 1-是',
  `is_published` TINYINT DEFAULT '0' COMMENT '是否发布 0-草稿 1-发布',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT '0' COMMENT '逻辑删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_title` (`title`) COMMENT '标题索引 - 支持标题搜索',
  KEY `idx_category_id` (`category_id`) COMMENT '分类 ID 索引 - 支持分类查询',
  KEY `idx_author` (`author`) COMMENT '作者索引 - 支持作者文章查询',
  KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引 - 支持时间排序',
  KEY `idx_view_count` (`view_count`) COMMENT '浏览量索引 - 支持热门文章查询',
  KEY `idx_is_published` (`is_published`) COMMENT '发布状态索引 - 过滤已发布文章',
  KEY `idx_category_published` (`category_id`, `is_published`) COMMENT '联合索引 - 分类下已发布文章',
  KEY `idx_create_published` (`is_published`, `create_time`) COMMENT '联合索引 - 已发布文章按时间排序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- =====================================================
-- 2. 文章分类表 blog_category
-- =====================================================
DROP TABLE IF EXISTS `blog_category`;
CREATE TABLE `blog_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `slug` VARCHAR(50) NOT NULL COMMENT '分类 Slug（URL 友好）',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
  `sort_order` INT DEFAULT '0' COMMENT '排序权重',
  `article_count` INT DEFAULT '0' COMMENT '文章数量（冗余字段）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`) COMMENT '分类名称唯一索引',
  UNIQUE KEY `uk_slug` (`slug`) COMMENT 'Slug 唯一索引',
  KEY `idx_sort_order` (`sort_order`) COMMENT '排序索引 - 支持分类列表排序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章分类表';

-- =====================================================
-- 3. 文章标签表 blog_tag
-- =====================================================
DROP TABLE IF EXISTS `blog_tag`;
CREATE TABLE `blog_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签 ID',
  `name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `slug` VARCHAR(50) NOT NULL COMMENT '标签 Slug',
  `article_count` INT DEFAULT '0' COMMENT '文章数量（冗余字段）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`) COMMENT '标签名称唯一索引',
  UNIQUE KEY `uk_slug` (`slug`) COMMENT 'Slug 唯一索引',
  KEY `idx_article_count` (`article_count`) COMMENT '文章数索引 - 支持热门标签查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签表';

-- =====================================================
-- 4. 文章 - 标签关联表 blog_article_tag
-- =====================================================
DROP TABLE IF EXISTS `blog_article_tag`;
CREATE TABLE `blog_article_tag` (
  `article_id` BIGINT NOT NULL COMMENT '文章 ID',
  `tag_id` BIGINT NOT NULL COMMENT '标签 ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`article_id`, `tag_id`),
  KEY `idx_tag_id` (`tag_id`) COMMENT '标签 ID 索引 - 支持标签文章查询',
  KEY `idx_article_id` (`article_id`) COMMENT '文章 ID 索引',
  CONSTRAINT `fk_article_tag_article` FOREIGN KEY (`article_id`) REFERENCES `blog_article` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_article_tag_tag` FOREIGN KEY (`tag_id`) REFERENCES `blog_tag` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章 - 标签关联表';

-- =====================================================
-- 5. 文章评论表 blog_comment
-- =====================================================
DROP TABLE IF EXISTS `blog_comment`;
CREATE TABLE `blog_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论 ID',
  `article_id` BIGINT NOT NULL COMMENT '文章 ID',
  `user_name` VARCHAR(50) NOT NULL COMMENT '用户名',
  `user_email` VARCHAR(100) DEFAULT NULL COMMENT '用户邮箱',
  `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `parent_id` BIGINT DEFAULT '0' COMMENT '父评论 ID（0-顶级评论）',
  `like_count` INT DEFAULT '0' COMMENT '点赞数',
  `is_audit` TINYINT DEFAULT '0' COMMENT '是否审核 0-待审核 1-通过 2-拒绝',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '评论 IP',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`) COMMENT '文章 ID 索引 - 支持文章评论查询',
  KEY `idx_parent_id` (`parent_id`) COMMENT '父评论 ID 索引 - 支持回复查询',
  KEY `idx_create_time` (`create_time`) COMMENT '创建时间索引 - 支持时间排序',
  KEY `idx_article_audit_time` (`article_id`, `is_audit`, `create_time`) COMMENT '联合索引 - 已审核评论按时间排序',
  KEY `idx_user_email` (`user_email`) COMMENT '用户邮箱索引 - 支持用户评论查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章评论表';

-- =====================================================
-- 初始化数据
-- =====================================================

-- 插入分类数据
INSERT INTO `blog_category` (`name`, `slug`, `description`, `sort_order`) VALUES
('Java 技术', 'java-tech', 'Java 相关技术文章', 1),
('前端开发', 'frontend-dev', '前端开发技术文章', 2),
('数据库', 'database', '数据库技术文章', 3),
('架构设计', 'architecture', '系统架构设计文章', 4),
('职场心得', 'career', '职场经验分享', 5);

-- 插入标签数据
INSERT INTO `blog_tag` (`name`, `slug`) VALUES
('SpringBoot', 'springboot'),
('Redis', 'redis'),
('MySQL', 'mysql'),
('微服务', 'microservices'),
('Vue', 'vue'),
('React', 'react'),
('Docker', 'docker'),
('Kubernetes', 'kubernetes'),
('JVM', 'jvm'),
('多线程', 'multithreading');

-- 插入示例文章
INSERT INTO `blog_article` (`title`, `summary`, `content`, `category_id`, `author`, `is_published`, `is_top`) VALUES
('SpringBoot 整合 Redis 实现缓存优化', '详细介绍 SpringBoot 如何整合 Redis 实现缓存优化，包括配置、序列化、缓存策略等', '正文内容...', 1, '张三', 1, 1),
('MySQL 索引优化实战指南', '从实际案例出发，讲解 MySQL 索引优化的技巧和最佳实践', '正文内容...', 3, '李四', 1, 0),
('Redis 缓存穿透、雪崩、击穿解决方案', '全面解析 Redis 缓存问题的成因和解决方案', '正文内容...', 3, '张三', 1, 0);

-- 插入示例评论
INSERT INTO `blog_comment` (`article_id`, `user_name`, `user_email`, `content`, `is_audit`) VALUES
(1, '用户 A', 'userA@example.com', '文章写得很好，学到了很多！', 1),
(1, '用户 B', 'userB@example.com', '感谢分享，非常实用！', 1),
(2, '用户 C', 'userC@example.com', '索引优化确实很重要，已实践！', 1);
