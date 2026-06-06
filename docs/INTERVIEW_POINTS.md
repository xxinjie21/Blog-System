# 面试考点总结

## 一、Redis 缓存设计

### 1. 缓存穿透
**问题**：查询不存在的数据，缓存未命中，请求直达数据库
**解决方案**：
- 空值缓存：即使数据库没有，也缓存一个 null 值
- 短 TTL：设置较短过期时间（如 5 分钟）
- 布隆过滤器：使用 BloomFilter 预判 Key 是否存在

**本项目实现**：
```java
// ArticleCache.java - 缓存空值防止穿透
public Object getArticleDetail(Long articleId) {
    Object cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        return cached;
    }
    // 查询数据库后，即使为 null 也缓存
    redisTemplate.opsForValue().set(key, article, 300, TimeUnit.SECONDS);
}
```

### 2. 缓存雪崩
**问题**：大量缓存同时过期，请求瞬间直达数据库
**解决方案**：
- 随机 TTL：在基础过期时间上增加随机值
- 多级缓存：Redis + 本地缓存（Caffeine）
- 高可用：Redis 集群部署

**本项目实现**：
```java
// ArticleCache.java - 随机过期时间
int randomTtl = articleDetailTtl + (int) (Math.random() * 600 - 300);
redisTemplate.opsForValue().set(key, articleDetail, randomTtl, TimeUnit.SECONDS);
```

### 3. 缓存击穿
**问题**：热点 Key 过期，大量并发请求击穿到数据库
**解决方案**：
- 互斥锁：使用分布式锁（Redis SETNX）
- 逻辑过期：不设置物理过期时间，后台线程更新
- 永不过期：设置永久 TTL，手动更新

**本项目实现**：
```java
// 热点文章缓存 - 定时主动刷新
@Scheduled(cron = "0 0/10 * * * ?")
public void refreshHotArticles() {
    // 每 10 分钟主动刷新热点文章缓存
}
```

### 4. 数据结构选型
| 场景 | 数据结构 | 原因 |
|------|----------|------|
| 浏览量计数 | String | INCR 原子操作，高性能 |
| 点赞用户 | Set | SISMEMBER 防重复点赞 |
| 热点排行 | ZSet | ZREVRANGE 支持动态排序 |
| 文章详情 | String | 简单 KV 存储 |

## 二、数据库索引优化

### 1. 索引设计原则
- 高频查询字段建立索引
- WHERE 子句字段建立索引
- ORDER BY 字段建立索引
- 避免在索引列上使用函数

### 2. 复合索引最左前缀匹配
```sql
-- 复合索引：idx_category_published (category_id, is_published)

-- ✅ 可以使用索引
WHERE category_id = 1 AND is_published = 1
WHERE category_id = 1

-- ❌ 不能使用索引
WHERE is_published = 1
```

### 3. 索引覆盖
```sql
-- 覆盖索引：只查询索引列，无需回表
SELECT id, title FROM blog_article WHERE category_id = 1

-- 非覆盖索引：需要回表
SELECT * FROM blog_article WHERE category_id = 1
```

### 4. 慢查询优化
```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- 查看慢查询
SELECT * FROM mysql.slow_log;

-- 使用 EXPLAIN 分析
EXPLAIN SELECT * FROM blog_article WHERE category_id = 1;
```

## 三、高并发处理

### 1. Redis 原子操作
```java
// 浏览量 +1（线程安全）
Long newCount = redisTemplate.opsForValue().increment(key);

// 点赞 +1（线程安全）
Boolean success = redisTemplate.opsForSet().add(userKey, userId);
```

### 2. 异步持久化
```java
// Quartz 定时任务 - 每 5 分钟同步一次
@Scheduled(cron = "0 0/5 * * * ?")
public void persistViewCounts() {
    Map<Long, Long> viewCounts = viewCountCache.getAllViewCounts();
    // 批量更新到 MySQL
}
```

### 3. 批量更新优化
```java
// 批量更新浏览量（减少 DB 交互）
for (Map.Entry<Long, Long> entry : viewCounts.entrySet()) {
    Article article = getById(entry.getKey());
    article.setViewCount(article.getViewCount() + entry.getValue());
    updateById(article);
}
```

## 四、Spring 核心

### 1. SpringBoot 自动装配
```java
@SpringBootApplication
// 等价于：
@Configuration
@EnableAutoConfiguration
@ComponentScan
```

### 2. AOP 应用场景
- 全局异常处理（@RestControllerAdvice）
- 日志记录
- 事务管理（@Transactional）
- 权限校验

### 3. 事务管理
```java
@Transactional(rollbackFor = Exception.class)
public void updateArticle(ArticleDTO dto) {
    // 更新文章
    updateById(article);
    // 更新标签关联
    articleTagMapper.deleteByArticleId(article.getId());
    // 清除缓存
    articleCache.deleteArticleDetail(article.getId());
}
```

### 4. 参数校验
```java
@PostMapping
public Result<Long> publish(@RequestBody @Validated ArticleDTO dto) {
    // 自动校验参数
}

// DTO 中使用校验注解
@NotBlank(message = "文章标题不能为空")
@Size(max = 200, message = "标题长度不能超过 200")
private String title;
```

## 五、RESTful 接口设计

### 1. 资源命名
```
✅ /api/articles        # 复数名词
❌ /api/getArticles     # 动词
```

### 2. HTTP 方法
| 操作 | 方法 | 路径 |
|------|------|------|
| 查询列表 | GET | /articles |
| 查询详情 | GET | /articles/{id} |
| 创建资源 | POST | /articles |
| 更新资源 | PUT | /articles |
| 删除资源 | DELETE | /articles/{id} |

### 3. 统一响应格式
```java
{
    "code": 200,
    "message": "操作成功",
    "data": {...},
    "timestamp": 1234567890
}
```

## 六、性能优化指标

| 指标 | 目标值 | 实现方案 |
|------|--------|----------|
| 首页响应 | < 100ms | 热点文章缓存 |
| 详情页 | < 200ms | 详情 + 评论缓存 |
| 浏览量 QPS | > 5000 | Redis INCR |
| DB 访问降低 | > 80% | 缓存命中率>90% |

## 七、项目亮点总结

1. **Redis 缓存优化**：浏览量、点赞数全部使用 Redis 原子操作
2. **异步持久化**：定时任务批量同步数据到 MySQL
3. **索引优化**：8 个索引覆盖所有查询场景
4. **复合索引**：idx_category_published 优化分类查询
5. **防重复点赞**：Redis Set 结构保证唯一性
6. **热点文章**：ZSet 动态排序，支持实时更新
7. **缓存一致性**：更新文章时主动清除缓存
8. **全局异常**：统一异常处理，规范响应格式

## 八、面试常见问题

### Q1: 如何保证 Redis 和 MySQL 数据一致性？
**答**：
1. 更新文章时主动清除缓存
2. 定时任务异步持久化 Redis 数据
3. 读取时采用 Cache-Aside 模式

### Q2: 浏览量为什么用 Redis 而不用 MySQL？
**答**：
1. MySQL 行锁竞争严重，并发性能差
2. Redis INCR 是原子操作，单线程保证一致性
3. Redis 单 QPS 可达 5000+，MySQL 只有几百

### Q3: 如何防止用户重复点赞？
**答**：
1. 使用 Redis Set 存储点赞用户
2. 点赞前使用 SISMEMBER 判断是否已点赞
3. Set 结构天然去重，保证唯一性

### Q4: 缓存雪崩如何解决？
**答**：
1. 设置随机过期时间（基础时间 ± 随机值）
2. 多级缓存：Redis + 本地缓存
3. 热点数据永不过期，后台线程更新

### Q5: 复合索引如何使用？
**答**：
1. 遵循最左前缀匹配原则
2. WHERE 条件包含索引最左列
3. 避免在索引列上使用函数或计算
