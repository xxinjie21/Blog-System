# 高性能缓存博客系统

<div align="center">

![JDK](https://img.shields.io/badge/JDK-1.8-blue.svg?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.15-brightgreen.svg?style=flat-square)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.3.1-orange.svg?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-6.2-red.svg?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat-square)
![Redisson](https://img.shields.io/badge/Redisson-3.23.5-critical.svg?style=flat-square)
![Quartz](https://img.shields.io/badge/Quartz-2.3.2-blue.svg?style=flat-square)

**基于 Spring Boot + MyBatis-Plus + Redisson + MySQL 的高性能博客后端系统**

[核心特性](#-核心特性) • [技术栈](#-技术栈) • [快速开始](#-快速开始) • [API 接口](#-api-接口) • [项目结构](#-项目结构)

</div>

---

## 项目介绍

本项目是一个 **高性能博客后端系统**，主打热点文章 Redis 缓存优化，降低 DB 访问压力。使用 Redisson 作为 Redis 客户端，实现了浏览量原子计数、点赞防重复、热点文章 TOP10 动态排序等核心功能。

### 核心理念

**Redis 缓存驱动，DB 访问降低 80%+。**

- 热点文章 ZSet 动态排序，首页响应 < 100ms
- 浏览量 Redisson AtomicLong 原子递增，Quartz 每 5 分钟异步持久化
- 点赞 RSet 防重复 + 分布式锁，异步落库
- 缓存过期随机偏移防雪崩

---

## 核心特性

### 1. 文章浏览量缓存

```
浏览请求 → Redisson AtomicLong INCR → Quartz 每 5 分钟批量同步到 MySQL
```

- `ViewCountCache` 使用 `RAtomicLong` 原子操作，避免 DB 行锁竞争
- `DataPersistJob`（QuartzJobBean）定时扫描 `blog:article:view:*` 批量 UPDATE
- 手动复位：持久化后 `setViewCount(0)`，下次从 DB 基准值重建

### 2. 热点文章 TOP10 缓存

```
Key: blog:hot:articles (ZSet)
Score: view_count
操作：ZREVRANGE 0 9 获取 TOP10
```

- `ArticleCache` 使用 `RScoredSortedSet` 动态排序
- 文章浏览量变更时实时更新 score
- 文章更新/删除时主动清除缓存
- 缓存过期时间 30 分钟（随机 ±300s 偏移防雪崩）

### 3. 点赞数据缓存

```
点赞计数：blog:article:like:{article_id} (RAtomicLong)
用户记录：blog:article:like:users:{article_id} (RSet)
分布式锁：lock:like:{article_id}:{userId} (RLock, 5s)
```

- `LikeCache` 使用 `RSet` 的 SISMEMBER 判断是否已点赞
- `RAtomicLong` INCR/DECR 原子操作
- 分布式锁防并发点赞/取消

### 4. 评论楼中楼

- `CommentVO` 嵌套 `replies: List<CommentVO>` 实现楼中楼回复
- 支持评论点赞

---

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 1.8 | Java 开发环境 |
| Spring Boot | 2.7.15 | 快速开发框架 |
| MyBatis-Plus | 3.5.3.1 | ORM 持久层框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 6.2 | 缓存中间件 |
| Redisson | 3.23.5 | Redis 客户端（AtomicLong / RSet / RLock / ZSet） |
| Quartz | 2.3.2 | 定时任务调度 |
| FastJSON2 | 2.0.32 | JSON 序列化 |
| Guava | 31.1-jre | 工具类 |
| Lombok | 1.18.12 | 简化代码 |

---

## 快速开始

### 1. 环境准备

```bash
# JDK 1.8
java -version

# Maven 3.6+
mvn -v
```

需要安装以下中间件：
- MySQL 8.0
- Redis 6.2

### 2. 克隆项目

```bash
git clone https://github.com/xxinjie21/High-Performance-Caching-Blog-System.git
cd High-Performance-Caching-Blog-System
```

### 3. 初始化数据库

```bash
mysql -u root -p < docs/init.sql
```

### 4. 修改配置

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
```

### 5. 启动项目

```bash
mvn clean install
mvn spring-boot:run
```

### 6. 访问接口

```
http://localhost:8080/articles/page
http://localhost:8080/articles/hot
```

---

## 数据库设计

### 表结构（5 张表，25+ 索引）

#### blog_article 文章表（15 列）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | PK，自增 |
| `title` | VARCHAR(200) | 索引 |
| `summary` | VARCHAR(500) | 摘要 |
| `content` | LONGTEXT | 正文 |
| `cover_image` | VARCHAR(500) | 封面图 |
| `category_id` | BIGINT | 分类索引 |
| `author` | VARCHAR(50) | 作者索引 |
| `view_count` | INT | 浏览量索引 |
| `like_count` | INT | 点赞数 |
| `comment_count` | INT | 评论数 |
| `is_top` | TINYINT | 置顶 |
| `is_published` | TINYINT | 发布状态索引 |
| `deleted` | TINYINT | 逻辑删除 |

**复合索引**: `(category_id, is_published)`, `(is_published, create_time)`

#### blog_category 分类表（8 列）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | PK |
| `name` | VARCHAR(50) | 唯一索引 |
| `slug` | VARCHAR(50) | 唯一索引 |
| `sort_order` | INT | 排序索引 |
| `article_count` | INT | 文章计数（反范式） |

#### blog_tag 标签表（5 列）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | PK |
| `name` | VARCHAR(50) | 唯一索引 |
| `slug` | VARCHAR(50) | 唯一索引 |
| `article_count` | INT | 热门标签索引 |

#### blog_article_tag 文章标签关联表（3 列）

| 字段 | 类型 | 说明 |
|------|------|------|
| `article_id` | BIGINT | 复合 PK，FK CASCADE |
| `tag_id` | BIGINT | 复合 PK，FK CASCADE |

#### blog_comment 评论表（11 列）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT | PK |
| `article_id` | BIGINT | 索引 |
| `user_name` | VARCHAR(50) | |
| `content` | VARCHAR(1000) | |
| `parent_id` | BIGINT | 回复查询索引（0=顶层） |
| `like_count` | INT | |
| `is_audit` | TINYINT | 审核状态 |

**复合索引**: `(article_id, is_audit, create_time)`

---

## API 接口

### 文章接口（9 个）

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/articles/page` | 分页查询文章（支持 title/category/author/时间筛选） |
| GET | `/articles/{id}` | 文章详情（Cache-Aside：Redis → DB） |
| POST | `/articles` | 发布文章（@Transactional + 清除热点缓存） |
| PUT | `/articles` | 更新文章（清除详情 + 热点缓存） |
| DELETE | `/articles/{id}` | 逻辑删除（清除全部相关缓存） |
| GET | `/articles/hot` | 热点文章 TOP10（Redis ZSet） |
| POST | `/articles/{id}/view` | 增加浏览量（AtomicLong INCR） |
| POST | `/articles/{id}/like` | 点赞（分布式锁 + Set 防重复） |
| DELETE | `/articles/{id}/like` | 取消点赞 |

### 分类 / 标签 / 评论（5 个）

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/categories/list` | 分类列表（按 sort_order DESC） |
| GET | `/tags/hot` | 热门标签 TOP N |
| GET | `/comments/article/{articleId}` | 文章评论（楼中楼结构） |
| POST | `/comments` | 添加评论 |
| DELETE | `/comments/{id}` | 删除评论 |
| POST | `/comments/{id}/like` | 评论点赞 |

---

## Redis 缓存设计

| 数据结构 | Key 模式 | 用途 | TTL |
|---------|---------|------|-----|
| ZSet | `blog:hot:articles` | 热点文章排行 | 30min（随机偏移） |
| AtomicLong | `blog:article:view:{id}` | 浏览量计数 | 永久（定时复位） |
| AtomicLong | `blog:article:like:{id}` | 点赞计数 | 永久（定时复位） |
| Set | `blog:article:like:users:{id}` | 点赞用户记录 | 永久 |
| RBucket | `blog:article:detail:{id}` | 文章详情缓存 | 3600s（随机偏移） |
| RLock | `lock:like:{articleId}:{userId}` | 点赞分布式锁 | 5s 超时 |

---

## 项目结构

```
High-Performance-Caching-Blog-System/
├── src/main/java/com/blog/
│   ├── BlogApplication.java          # 启动类(@EnableScheduling, @EnableAspectJAutoProxy)
│   ├── cache/                         # 缓存策略层（核心）
│   │   ├── ArticleCache.java          # 文章详情 + 热点 ZSet + 文章 ID 缓存
│   │   ├── ViewCountCache.java        # AtomicLong 浏览量
│   │   └── LikeCache.java             # AtomicLong + RSet + 分布式锁
│   ├── config/
│   │   ├── RedisConfig.java           # RedisTemplate + CacheManager + @EnableCaching
│   │   ├── RedissonConfig.java        # RedissonClient 单机模式
│   │   ├── MybatisPlusConfig.java     # 分页拦截器
│   │   ├── MyMetaObjectHandler.java   # createTime/updateTime 自动填充
│   │   ├── QuartzConfig.java          # DataPersistJob cron: "0 0/5 * * * ?"
│   │   └── WebConfig.java             # CORS 全开放
│   ├── controller/                    # 4 个 Controller，14 个接口
│   ├── dto/                           # ArticleDTO, ArticleQueryDTO, CommentDTO
│   ├── entity/                        # 5 实体（Article, Category, Tag, Comment, ArticleTag）
│   ├── exception/                     # BlogException + GlobalExceptionHandler
│   ├── job/
│   │   └── DataPersistJob.java        # QuartzJobBean + ApplicationContextAware
│   ├── mapper/ + mapper/*.xml         # 5 Mapper + 3 XML
│   ├── service/impl/                  # 4 Service + 实现
│   ├── util/RedisUtil.java            # Redis String/Key/Set/ZSet 操作
│   └── vo/                            # 5 VO（ArticleVO, ArticleDetailVO, HotArticleVO, TagVO, CommentVO）
├── docs/
│   ├── init.sql                       # 建表 + 索引 + 种子数据
│   └── INTERVIEW_POINTS.md            # 面试复习指南
├── PROJECT_STRUCTURE.md               # 项目结构详解
└── README.md
```

---

## 技术特点

| 特点 | 说明 |
|------|------|
| **Redisson 全面使用** | 不用 Spring RedisTemplate，全部通过 Redisson AtomicLong/RSet/RLock/RBucket 操作 |
| **异步持久化** | Quartz 每 5 分钟将 Redis 浏览量/点赞数批量同步到 MySQL |
| **ZSet 动态排序** | 热点文章实时更新 score，ZREVRANGE 获取 TOP10 |
| **Set 防重复点赞** | SISMEMBER O(1) 判断 + 分布式锁防并发 |
| **缓存随机偏移** | TTL ±300s 随机偏移，防止缓存雪崩 |
| **QuartzJobBean** | `ApplicationContextAware` 手动获取 Bean，解决 Quartz 无法注入 Spring Bean 问题 |
| **楼中楼评论** | `CommentVO.replies` 嵌套子评论列表 |
| **复合索引** | 文章表 3 个复合索引，评论表 1 个复合索引 |

---

## 常见问题

### Q: 项目启动失败？

检查 MySQL、Redis 是否启动，检查 `application.yml` 配置。

### Q: 缓存和数据库数据不一致？

使用 Cache-Aside 模式：先更新 DB，再删除缓存。Quartz 定时任务兜底同步。

### Q: 热点文章缓存过期后怎么办？

缓存过期后自动从 DB 加载并重建缓存，30 分钟后再次过期。

---

## 许可证

MIT License

---

<div align="center">

**如果本项目对你有帮助，请给个 Star 支持！**

</div>
