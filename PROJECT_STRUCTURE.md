# 高性能缓存博客系统 - 项目结构与数据库设计

## 📁 项目目录结构

```
High-Performance-Caching-Blog-System/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── blog/
│   │   │           ├── BlogApplication.java          # SpringBoot 启动类
│   │   │           ├── config/                       # 配置类
│   │   │           │   ├── RedisConfig.java          # Redis 配置（连接池、序列化）
│   │   │           │   ├── MybatisPlusConfig.java    # MP 分页插件配置
│   │   │           │   └── WebConfig.java            # Web 配置（跨域、拦截器）
│   │   │           ├── controller/                   # 控制层
│   │   │           │   ├── ArticleController.java    # 文章管理
│   │   │           │   ├── CategoryController.java   # 分类管理
│   │   │           │   ├── TagController.java        # 标签管理
│   │   │           │   └── CommentController.java    # 评论管理
│   │   │           ├── service/                      # 业务层
│   │   │           │   ├── ArticleService.java       # 文章服务接口
│   │   │           │   ├── impl/
│   │   │           │   │   └── ArticleServiceImpl.java
│   │   │           │   ├── CategoryService.java
│   │   │           │   ├── TagService.java
│   │   │           │   └── CommentService.java
│   │   │           ├── mapper/                       # DAO 层
│   │   │           │   ├── ArticleMapper.java
│   │   │           │   ├── CategoryMapper.java
│   │   │           │   ├── TagMapper.java
│   │   │           │   └── CommentMapper.java
│   │   │           ├── entity/                       # 实体类
│   │   │           │   ├── Article.java
│   │   │           │   ├── Category.java
│   │   │           │   ├── Tag.java
│   │   │           │   └── Comment.java
│   │   │           ├── dto/                          # 数据传输对象
│   │   │           │   ├── ArticleDTO.java
│   │   │           │   ├── ArticleQueryDTO.java
│   │   │           │   └── CommentDTO.java
│   │   │           ├── vo/                           # 视图对象
│   │   │           │   ├── ArticleVO.java
│   │   │           │   ├── ArticleDetailVO.java
│   │   │           │   └── HotArticleVO.java
│   │   │           ├── cache/                        # 缓存策略层（核心）
│   │   │           │   ├── ArticleCache.java         # 文章缓存策略
│   │   │           │   ├── HotArticleCache.java      # 热点文章缓存
│   │   │           │   ├── ViewCountCache.java       # 浏览量缓存
│   │   │           │   └── LikeCache.java            # 点赞缓存
│   │   │           └── util/                         # 工具类
│   │   │               └── RedisUtil.java            # Redis 工具封装
│   │   └── resources/
│   │       ├── application.yml                       # 主配置文件
│   │       ├── application-dev.yml                   # 开发环境配置
│   │       ├── application-prod.yml                  # 生产环境配置
│   │       └── mapper/                               # MyBatis XML
│   │           ├── ArticleMapper.xml
│   │           ├── CategoryMapper.xml
│   │           ├── TagMapper.xml
│   │           └── CommentMapper.xml
│   └── test/
│       └── java/
│           └── com/
│               └── blog/
│                   ├── controller/
│                   ├── service/
│                   └── cache/
│                       ├── ArticleCacheTest.java
│                       └── HotArticleCacheTest.java
├── pom.xml                                           # Maven 依赖管理
├── README.md                                         # 项目说明
└── docs/                                             # 文档目录
    ├── API.md                                        # 接口文档
    └── OPTIMIZATION.md                               # 优化方案说明
```

---

## 🗄️ 数据库表设计（4 张核心表）

### 1. 文章表 `blog_article`

```sql
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
```

**索引设计说明：**
- `idx_title`: 支持 `WHERE title LIKE '%关键词%'` 搜索（前缀匹配可用 `LIKE '关键词%'`）
- `idx_category_id`: 支持按分类查询文章
- `idx_create_time`: 支持按发布时间排序
- `idx_view_count`: 支持热门文章排行榜查询
- `idx_category_published`: 复合索引，优化 `WHERE category_id=? AND is_published=1 ORDER BY create_time DESC`
- `idx_create_published`: 复合索引，优化最新发布文章查询

---

### 2. 分类表 `blog_category`

```sql
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
```

**索引设计说明：**
- `uk_name`: 唯一索引，防止分类名称重复
- `uk_slug`: 唯一索引，SEO 友好的 URL 标识
- `idx_sort_order`: 支持按权重排序分类

---

### 3. 标签表 `blog_tag`

```sql
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

-- 文章 - 标签关联表（多对多关系）
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
```

**索引设计说明：**
- `uk_name`: 唯一索引，防止标签重复
- `idx_article_count`: 支持热门标签查询（`ORDER BY article_count DESC`）
- `blog_article_tag`: 联合主键 + 双向索引，支持正向和反向查询

---

### 4. 评论表 `blog_comment`

```sql
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
```

**索引设计说明：**
- `idx_article_id`: 查询某文章的所有评论
- `idx_parent_id`: 查询某评论的回复（楼中楼）
- `idx_article_audit_time`: 复合索引，优化 `WHERE article_id=? AND is_audit=1 ORDER BY create_time`
- `idx_create_time`: 支持最新评论查询

---

## 🔥 Redis 缓存方案

### 一、文章浏览量缓存策略

**设计目标：** 高频更新，异步持久化，降低 DB 压力

#### 1. 数据结构设计

```redis
# 单篇文章浏览量（String 结构）
Key: blog:article:view:{article_id}
Value: 浏览量数值（String 存储）
TTL: 永不过期（手动管理）

# 示例：
SET blog:article:view:1001 1580

# 浏览量 +1（原子操作）
INCR blog:article:view:1001
```

#### 2. 缓存策略

```yaml
缓存更新策略：
  - 读操作：先读 Redis，不存在则读 DB 并回写 Redis
  - 写操作：只更新 Redis，定时任务异步持久化到 MySQL
  - 持久化频率：每 5 分钟同步一次增量数据
  - 崩溃恢复：基于 MySQL 的 view_count 基础值 + Redis 增量值
```

#### 3. 伪代码逻辑

```
# 获取文章浏览量
def get_view_count(article_id):
    redis_key = f"blog:article:view:{article_id}"
    view_count = redis.get(redis_key)
    
    if view_count is None:
        # 从 DB 读取基础值
        view_count = db.query("SELECT view_count FROM blog_article WHERE id=?", article_id)
        redis.set(redis_key, view_count)
    
    return view_count

# 增加浏览量
def increment_view_count(article_id):
    redis_key = f"blog:article:view:{article_id}"
    return redis.incr(redis_key)

# 定时持久化（每 5 分钟）
def persist_to_db():
    for article_id in redis.keys("blog:article:view:*"):
        view_count = redis.get(article_id)
        db.execute("UPDATE blog_article SET view_count=? WHERE id=?", view_count, article_id)
```

---

### 二、热点 TOP10 文章缓存

**设计目标：** 快速响应首页/侧边栏热门文章请求

#### 1. 数据结构设计

```redis
# 方案 A：ZSet（推荐）- 支持动态排序
Key: blog:hot:articles
Value: article_id（member），view_count（score）
TTL: 30 分钟（定时刷新）

# 获取 TOP10
ZREVRANGE blog:hot:articles 0 9 WITHSCORES

# 方案 B：List - 简单缓存
Key: blog:hot:articles:list
Value: [article_id1, article_id2, ..., article_id10]
TTL: 10 分钟
```

#### 2. 缓存更新策略

```yaml
更新策略：
  - 被动更新：文章浏览量变化时，更新 ZSet 分数
  - 主动更新：定时任务每 10 分钟重新计算 TOP10
  - 缓存预热：系统启动时从 DB 加载 TOP10 到 Redis
  
触发条件：
  - 文章发布/下架：重新计算热点列表
  - 浏览量阈值：单篇文章 1 小时内增长>100 次浏览，触发更新
```

#### 3. 伪代码逻辑

```
# 添加文章到热点列表
def add_to_hot_articles(article_id, view_count):
    redis.zadd("blog:hot:articles", {article_id: view_count})
    
# 获取 TOP10 热点文章
def get_hot_articles():
    # 获取 TOP10 文章 ID
    article_ids = redis.zrevrange("blog:hot:articles", 0, 9)
    
    # 批量获取文章详情（Pipeline 优化）
    articles = []
    pipeline = redis.pipeline()
    for article_id in article_ids:
        pipeline.get(f"blog:article:detail:{article_id}")
    article_details = pipeline.execute()
    
    return article_details

# 定时刷新热点文章
def refresh_hot_articles():
    # 从 DB 查询最新的 TOP10
    hot_articles = db.query("""
        SELECT id, title, view_count 
        FROM blog_article 
        WHERE is_published = 1 
        ORDER BY view_count DESC 
        LIMIT 10
    """)
    
    # 清空旧数据
    redis.delete("blog:hot:articles")
    
    # 写入新数据
    for article in hot_articles:
        redis.zadd("blog:hot:articles", {article.id: article.view_count})
        # 同时缓存文章详情
        redis.setex(f"blog:article:detail:{article.id}", 1800, serialize(article))
```

---

### 三、点赞数据缓存策略

**设计目标：** 高频写入，防重复点赞，异步持久化

#### 1. 数据结构设计

```redis
# 文章点赞总数（String）
Key: blog:article:like:{article_id}
Value: 点赞总数
TTL: 永不过期

# 用户点赞记录（Set）- 防重复点赞
Key: blog:article:like:users:{article_id}
Value: {user_id1, user_id2, ...}
TTL: 永不过期

# 用户点赞的文章列表（可选 - 支持个人中心）
Key: blog:user:liked:{user_id}
Value: {article_id1, article_id2, ...}
TTL: 永不过期

# 示例：
# 获取文章点赞数
GET blog:article:like:1001

# 检查用户是否已点赞
SISMEMBER blog:article:like:users:1001 user_001
```

#### 2. 缓存策略

```yaml
点赞流程：
  1. 检查用户是否已点赞（SISMEMBER）
  2. 未点赞：点赞数 +1，用户 ID 加入 Set
  3. 已点赞：取消点赞（点赞数 -1，从 Set 移除）
  4. 异步持久化到 DB（消息队列或定时任务）
  
防刷限制：
  - 单用户单篇文章：只能点赞/取消一次
  - 单用户操作频率：1 秒内最多 1 次点赞（RATE LIMIT）
  - 单 IP 操作频率：1 分钟内最多 10 次点赞
```

#### 3. 伪代码逻辑

```
# 点赞文章
def like_article(article_id, user_id):
    like_key = f"blog:article:like:{article_id}"
    user_like_key = f"blog:article:like:users:{article_id}"
    
    # 检查是否已点赞
    is_liked = redis.sismember(user_like_key, user_id)
    
    if is_liked:
        # 取消点赞
        redis.decr(like_key)
        redis.srem(user_like_key, user_id)
        # 记录取消操作（异步持久化）
        async_persist_like(article_id, user_id, action='unlike')
        return False
    else:
        # 点赞
        redis.incr(like_key)
        redis.sadd(user_like_key, user_id)
        # 记录点赞操作（异步持久化）
        async_persist_like(article_id, user_id, action='like')
        return True

# 获取文章点赞数
def get_like_count(article_id):
    like_count = redis.get(f"blog:article:like:{article_id}")
    return int(like_count) if like_count else 0

# 检查用户是否点赞
def is_liked(article_id, user_id):
    return redis.sismember(f"blog:article:like:users:{article_id}", user_id)

# 异步持久化（消息队列）
def async_persist_like(article_id, user_id, action):
    # 发送到消息队列
    message_queue.publish("article_like", {
        "article_id": article_id,
        "user_id": user_id,
        "action": action,
        "timestamp": now()
    })
    
# 消费者持久化到 DB
def consume_like_messages():
    while True:
        message = message_queue.consume("article_like")
        if message.action == 'like':
            db.execute("""
                UPDATE blog_article 
                SET like_count = like_count + 1 
                WHERE id = ?
            """, message.article_id)
        else:
            db.execute("""
                UPDATE blog_article 
                SET like_count = like_count - 1 
                WHERE id = ?
            """, message.article_id)
```

---

## 📊 缓存 Key 设计规范

```yaml
命名规范：
  格式：业务：模块：类型：ID
  示例：
    - blog:article:view:1001          # 文章浏览量
    - blog:article:detail:1001        # 文章详情
    - blog:article:like:1001          # 文章点赞数
    - blog:article:like:users:1001    # 点赞用户列表
    - blog:hot:articles               # 热点文章榜
    - blog:category:list              # 分类列表
    - blog:tag:hot                    # 热门标签
    - blog:comment:article:1001       # 文章评论列表
    
TTL 策略：
  - 热点数据：10-30 分钟（定时刷新）
  - 基础数据：1-2 小时（低频变化）
  - 计数数据：永不过期（手动管理）
  - 用户会话：30 分钟（滑动过期）
```

---

## 🎯 性能优化指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 首页响应时间 | < 100ms | 热点文章全部缓存 |
| 文章详情页 | < 200ms | 详情 + 评论缓存 |
| 浏览量 QPS | > 5000 | Redis 原子操作 |
| DB 访问降低 | > 80% | 缓存命中率>90% |
| 点赞延迟 | < 50ms | 异步持久化 |

---

## 📝 总结

本方案通过以下设计实现高性能缓存：

1. **索引优化**：标题、分类、时间字段建立索引，避免全表扫描
2. **浏览量缓存**：Redis INCR 原子操作，异步持久化降低 DB 压力
3. **热点文章**：ZSet 动态排序 + 定时刷新，快速响应 TOP10 查询
4. **点赞系统**：Set 防重复 + 异步队列，支持高并发写入
5. **Key 规范**：统一命名空间，便于管理和监控
