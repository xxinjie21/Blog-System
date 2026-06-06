# 高性能缓存博客系统

基于 **SpringBoot + MyBatis-Plus + Redis + MySQL** 的高性能博客后端系统，主打热点文章 Redis 缓存优化，降低 DB 访问压力。

## 📋 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 1.8 | Java 开发环境 |
| SpringBoot | 2.7.15 | 快速开发框架 |
| MyBatis-Plus | 3.5.3.1 | ORM 持久层框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 6.2 | 缓存中间件 |
| Quartz | 2.3.2 | 定时任务调度 |
| Lombok | 1.18.28 | 简化代码 |

## 🚀 核心功能

### 1. 文章管理
- ✅ 文章发布/编辑/删除
- ✅ 文章分类管理
- ✅ 文章标签管理
- ✅ 文章分页多条件查询

### 2. 互动功能
- ✅ 文章点赞（Redis 计数 + 异步持久化）
- ✅ 文章评论（楼中楼回复）
- ✅ 浏览量统计（Redis 实时计数）

### 3. 首页功能
- ✅ 热点文章 TOP10（Redis ZSet 缓存）
- ✅ 文章分页查询
- ✅ 分类/标签筛选

## 📁 项目结构

```
High-Performance-Caching-Blog-System/
├── src/main/java/com/blog/
│   ├── BlogApplication.java          # 启动类
│   ├── config/                       # 配置类
│   │   ├── RedisConfig.java          # Redis 配置
│   │   ├── MybatisPlusConfig.java    # MP 配置
│   │   ├── WebConfig.java            # Web 配置
│   │   └── QuartzConfig.java         # 定时任务配置
│   ├── controller/                   # 控制层
│   │   ├── ArticleController.java
│   │   ├── CategoryController.java
│   │   ├── TagController.java
│   │   └── CommentController.java
│   ├── service/                      # 业务层
│   │   └── impl/
│   ├── mapper/                       # DAO 层
│   ├── entity/                       # 实体类
│   ├── dto/                          # 数据传输对象
│   ├── vo/                           # 视图对象
│   ├── cache/                        # 缓存策略层（核心）
│   │   ├── ArticleCache.java
│   │   ├── ViewCountCache.java
│   │   └── LikeCache.java
│   ├── job/                          # 定时任务
│   │   └── DataPersistJob.java
│   ├── util/                         # 工具类
│   │   └── RedisUtil.java
│   └── exception/                    # 异常处理
│       ├── BlogException.java
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml               # 配置文件
│   └── mapper/                       # MyBatis XML
└── docs/
    └── init.sql                      # 数据库初始化脚本
```

## 🔥 Redis 缓存方案

### 1. 文章浏览量缓存
```redis
Key: blog:article:view:{article_id}
Value: 浏览量数值
操作：INCR 原子递增
持久化：每 5 分钟同步到 MySQL
```

**优化点：**
- Redis INCR 原子操作，避免 DB 行锁竞争
- 异步持久化，减少 DB 压力
- 崩溃恢复：DB 基础值 + Redis 增量值

### 2. 热点文章 TOP10 缓存
```redis
Key: blog:hot:articles
Type: ZSet
Member: article_id
Score: view_count
操作：ZREVRANGE 获取 TOP10
```

**优化点：**
- ZSet 动态排序，支持实时更新
- 缓存过期时间 30 分钟（防雪崩）
- 文章更新时主动清除缓存

### 3. 点赞数据缓存
```redis
# 点赞总数
Key: blog:article:like:{article_id}
Type: String

# 用户点赞记录（防重复）
Key: blog:article:like:users:{article_id}
Type: Set
Member: user_id
```

**优化点：**
- Set 判断是否已点赞，防止重复
- INCR/DECR 原子操作
- 异步消息队列持久化

## 📊 数据库索引设计

### 文章表索引
```sql
idx_title              # 标题搜索
idx_category_id        # 分类查询
idx_author             # 作者查询
idx_create_time        # 时间排序
idx_view_count         # 热门查询
idx_is_published       # 发布状态过滤
idx_category_published # 复合索引（分类 + 发布）
idx_create_published   # 复合索引（发布 + 时间）
```

### 评论表索引
```sql
idx_article_id              # 文章评论查询
idx_parent_id               # 回复查询
idx_create_time             # 时间排序
idx_article_audit_time      # 复合索引（文章 + 审核 + 时间）
idx_user_email              # 用户查询
```

## 🔧 快速开始

### 1. 环境准备
```bash
# 安装 MySQL 8.0
# 安装 Redis 6.2
# 安装 JDK 1.8
```

### 2. 数据库初始化
```bash
mysql -u root -p < docs/init.sql
```

### 3. 修改配置
编辑 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog_system
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
```

### 4. 启动项目
```bash
mvn clean install
mvn spring-boot:run
```

### 5. 访问接口
```
http://localhost:8080/api/articles/page
http://localhost:8080/api/articles/hot
```

## 📝 接口文档

### 文章管理
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/articles/page | GET | 分页查询文章 |
| /api/articles/{id} | GET | 获取文章详情 |
| /api/articles | POST | 发布文章 |
| /api/articles | PUT | 更新文章 |
| /api/articles/{id} | DELETE | 删除文章 |
| /api/articles/hot | GET | 热点文章 TOP10 |
| /api/articles/{id}/view | POST | 增加浏览量 |
| /api/articles/{id}/like | POST | 点赞文章 |
| /api/articles/{id}/like | DELETE | 取消点赞 |

### 分类管理
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/categories/list | GET | 获取分类列表 |

### 标签管理
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/tags/hot | GET | 热门标签列表 |

### 评论管理
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/comments/article/{articleId} | GET | 获取文章评论 |
| /api/comments | POST | 添加评论 |
| /api/comments/{id} | DELETE | 删除评论 |
| /api/comments/{id}/like | POST | 点赞评论 |

## 🎯 性能优化指标

| 指标 | 目标值 | 实现方案 |
|------|--------|----------|
| 首页响应时间 | < 100ms | 热点文章全部缓存 |
| 文章详情页 | < 200ms | 详情 + 评论缓存 |
| 浏览量 QPS | > 5000 | Redis 原子操作 |
| DB 访问降低 | > 80% | 缓存命中率>90% |
| 点赞延迟 | < 50ms | 异步持久化 |

## 💡 面试考点

### 1. Redis 缓存
- 缓存穿透、雪崩、击穿解决方案
- Redis 数据结构选型（String/Set/ZSet）
- 缓存一致性保证策略

### 2. 数据库优化
- 索引设计原则
- 复合索引最左前缀匹配
- 慢查询优化

### 3. 高并发处理
- Redis 原子操作（INCR/DECR）
- 异步持久化（定时任务）
- 批量更新优化

### 4. Spring 相关
- SpringBoot 自动装配原理
- AOP 应用场景
- 事务管理（@Transactional）

## 📄 License

MIT License

## 👤 作者

本项目用于 Java 实习求职，展示 MySQL 索引优化、Redis 缓存设计能力。
