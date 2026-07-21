# 高性能缓存博客系统

<div align="center">

![JDK](https://img.shields.io/badge/JDK-1.8-blue.svg?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.15-brightgreen.svg?style=flat-square)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.3.1-orange.svg?style=flat-square)
![Redis](https://img.shields.io/badge/Redis-6.2-red.svg?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat-square)

**基于 SpringBoot + MyBatis-Plus + Redis + MySQL 的高性能博客后端系统**

[核心特性](#-核心特性) • [技术栈](#-技术栈) • [快速开始](#-快速开始) • [项目结构](#-项目结构) • [面试考点](#-面试考点)

</div>

---

## 项目介绍

本项目是一个 **高性能博客后端系统**，主打热点文章 Redis 缓存优化，降低 DB 访问压力。实现了文章管理、互动功能、首页热点推荐等核心功能。

### 核心理念

**Redis 缓存驱动，DB 访问降低 80%+。**

- 热点文章全部缓存，首页响应 < 100ms
- 浏览量 Redis 原子计数，异步持久化
- 点赞数据 Redis Set 防重复，异步落库
- 缓存命中率 > 90%

---

## 核心特性

### 1. 文章浏览量缓存

```
浏览请求 → Redis INCR 原子递增 → 定时任务每 5 分钟同步到 MySQL
```

- Redis INCR 原子操作，避免 DB 行锁竞争
- 异步持久化，减少 DB 压力
- 崩溃恢复：DB 基础值 + Redis 增量值

### 2. 热点文章 TOP10 缓存

```
Key: blog:hot:articles (ZSet)
Score: view_count
操作：ZREVRANGE 获取 TOP10
```

- ZSet 动态排序，支持实时更新
- 缓存过期时间 30 分钟（防雪崩）
- 文章更新时主动清除缓存

### 3. 点赞数据缓存

```
点赞总数：blog:article:like:{article_id} (String, INCR/DECR)
用户点赞记录：blog:article:like:users:{article_id} (Set, 防重复)
```

- Set 判断是否已点赞，防止重复
- INCR/DECR 原子操作
- 异步消息队列持久化

### 4. 评论管理

- 楼中楼回复
- Redis 缓存热门文章评论
- 分页查询优化

---

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 1.8 | Java 开发环境 |
| Spring Boot | 2.7.15 | 快速开发框架 |
| MyBatis-Plus | 3.5.3.1 | ORM 持久层框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 6.2 | 缓存中间件 |
| Quartz | 2.3.2 | 定时任务调度 |
| Lombok | 1.18.28 | 简化代码 |

### 核心依赖

- **Redis**：浏览量计数、热点文章排序、点赞防重复
- **Quartz**：定时持久化 Redis 数据到 MySQL
- **MyBatis-Plus**：简化 CRUD + 分页查询
- **Lombok**：简化实体类代码

---

## 快速开始

### 1. 环境准备

```bash
# JDK 1.8
java -v

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
    url: jdbc:mysql://localhost:3306/blog_system
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

## 功能详解

### 文章管理

| 接口 | 方法 | 说明 |
|------|------|------|
| /articles/page | GET | 分页查询文章 |
| /articles/{id} | GET | 获取文章详情 |
| /articles | POST | 发布文章 |
| /articles | PUT | 更新文章 |
| /articles/{id} | DELETE | 删除文章 |
| /articles/hot | GET | 热点文章 TOP10 |
| /articles/{id}/view | POST | 增加浏览量 |
| /articles/{id}/like | POST | 点赞文章 |
| /articles/{id}/like | DELETE | 取消点赞 |

### 分类 / 标签 / 评论

| 接口 | 方法 | 说明 |
|------|------|------|
| /categories/list | GET | 获取分类列表 |
| /tags/hot | GET | 热门标签列表 |
| /comments/article/{articleId} | GET | 获取文章评论 |
| /comments | POST | 添加评论 |
| /comments/{id} | DELETE | 删除评论 |
| /comments/{id}/like | POST | 点赞评论 |

### 数据库索引设计

```sql
-- 文章表索引
idx_title              # 标题搜索
idx_category_id        # 分类查询
idx_author             # 作者查询
idx_create_time        # 时间排序
idx_view_count         # 热门查询
idx_category_published # 复合索引（分类 + 发布）

-- 评论表索引
idx_article_id              # 文章评论查询
idx_parent_id               # 回复查询
idx_article_audit_time      # 复合索引（文章 + 审核 + 时间）
```

---

## 项目结构

```
High-Performance-Caching-Blog-System/
├── src/main/java/com/blog/
│   ├── BlogApplication.java          # 启动类
│   ├── config/                       # 配置类
│   │   ├── RedisConfig.java          # Redis 配置
│   │   ├── MybatisPlusConfig.java    # MP 配置
│   │   └── QuartzConfig.java         # 定时任务配置
│   ├── controller/                   # 控制层
│   ├── service/impl/                 # 业务层
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
│   ├── util/RedisUtil.java           # 工具类
│   └── exception/                    # 异常处理
├── src/main/resources/
│   ├── application.yml               # 配置文件
│   └── mapper/                       # MyBatis XML
└── docs/
    └── init.sql                      # 数据库初始化脚本
```

---

## 技术特点

| 特点 | 说明 |
|------|------|
| **Redis 原子操作** | 浏览量 INCR、点赞 INCR/DECR，避免 DB 行锁竞争 |
| **异步持久化** | 定时任务每 5 分钟将 Redis 数据同步到 MySQL |
| **ZSet 动态排序** | 热点文章实时更新，ZREVRANGE 获取 TOP10 |
| **Set 防重复** | 用户点赞记录存储在 Set 中，SISMEMBER 判断 |
| **缓存过期** | 30 分钟过期时间防雪崩 |
| **复合索引** | 文章表、评论表设计复合索引优化查询 |

---

## 面试考点

### 1. Redis 缓存

**Q1: 如何解决缓存穿透？**

**参考答案**：
> 1. **空值缓存**：查询结果为空时，缓存空值并设置短 TTL
> 2. **布隆过滤器**：前置过滤不存在的数据
> 3. **接口校验**：拦截非法请求

**Q2: Redis 数据结构如何选型？**

**参考答案**：
> 1. **String**：浏览量计数（INCR 原子操作）
> 2. **ZSet**：热点文章排序（按 score 排序）
> 3. **Set**：用户点赞记录（防重复，SISMEMBER 判断）

### 2. 数据库优化

**Q3: 索引设计原则？**

**参考答案**：
> 1. **最左前缀**：复合索引遵循最左前缀匹配
> 2. **区分度**：高区分度字段放前面
> 3. **覆盖索引**：尽量使用覆盖索引避免回表
> 4. **避免过多索引**：索引会降低写入性能

### 3. 高并发处理

**Q4: 浏览量如何保证不丢失？**

**参考答案**：
> 1. **Redis 原子递增**：INCR 命令保证原子性
> 2. **定时持久化**：Quartz 每 5 分钟同步到 MySQL
> 3. **崩溃恢复**：重启后 DB 基础值 + Redis 增量值
> 4. **异步写入**：不阻塞主流程

---

## 常见问题

### Q: 项目启动失败？

检查 MySQL、Redis 是否启动，检查 `application.yml` 配置是否正确。

### Q: 缓存和数据库数据不一致？

使用 Cache-Aside 模式：先更新 DB，再删除缓存。定时任务兜底同步。

### Q: 热点文章缓存过期后怎么办？

缓存过期后自动从 DB 加载并重建缓存，30 分钟后再次过期。

---

## 许可证

MIT License

---

<div align="center">

**如果本项目对你有帮助，请给个 Star 支持！**

</div>
