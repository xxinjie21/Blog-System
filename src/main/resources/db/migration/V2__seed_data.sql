-- =====================================================
-- V2: 种子数据（Flyway 版本 2）
-- 只在第一次迁移时执行一次；想重置就删库重建或 flyway clean
-- =====================================================

-- 插入分类数据
-- INSERT IGNORE：数据已存在（唯一索引 uk_name 冲突）时跳过，不报错，实现幂等
INSERT IGNORE INTO `blog_category` (`name`, `slug`, `description`, `sort_order`) VALUES
('Java 技术', 'java-tech', 'Java 相关技术文章', 1),
('前端开发', 'frontend-dev', '前端开发技术文章', 2),
('数据库', 'database', '数据库技术文章', 3),
('架构设计', 'architecture', '系统架构设计文章', 4),
('职场心得', 'career', '职场经验分享', 5);

-- 插入标签数据
-- INSERT IGNORE：同上，uk_name 冲突时跳过
INSERT IGNORE INTO `blog_tag` (`name`, `slug`) VALUES
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
-- INSERT IGNORE：本文无唯一索引，此处为保持脚本风格统一
INSERT IGNORE INTO `blog_article` (`title`, `summary`, `content`, `category_id`, `author`, `is_published`, `is_top`) VALUES
('SpringBoot 整合 Redis 实现缓存优化', '详细介绍 SpringBoot 如何整合 Redis 实现缓存优化，包括配置、序列化、缓存策略等', '正文内容...', 1, '张三', 1, 1),
('MySQL 索引优化实战指南', '从实际案例出发，讲解 MySQL 索引优化的技巧和最佳实践', '正文内容...', 3, '李四', 1, 0),
('Redis 缓存穿透、雪崩、击穿解决方案', '全面解析 Redis 缓存问题的成因和解决方案', '正文内容...', 3, '张三', 1, 0);

-- 插入示例评论
-- INSERT IGNORE：同上，无唯一索引，仅统一风格
INSERT IGNORE INTO `blog_comment` (`article_id`, `user_name`, `user_email`, `content`, `is_audit`) VALUES
(1, '用户 A', 'userA@example.com', '文章写得很好，学到了很多！', 1),
(1, '用户 B', 'userB@example.com', '感谢分享，非常实用！', 1),
(2, '用户 C', 'userC@example.com', '索引优化确实很重要，已实践！', 1);

-- 插入默认管理员账号（密码为 123456 的 BCrypt 值）
-- INSERT IGNORE：uk_username 冲突时跳过
INSERT IGNORE INTO `blog_user` (`username`, `password`, `nickname`, `email`, `phone`, `status`) VALUES
('admin', '$2b$10$obA9noReDE8Q4vzqukAVrOM3lLWzQ2uG4eRzaMJZjcs7ig7B2drh.', '管理员', 'admin@blog.com', '13800138000', 1);
