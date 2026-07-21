package com.blog.cache;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 文章缓存策略类（基于 Redisson）
 * 
 * 【面试考点】
 * 1. 缓存穿透：查询不存在的文章，使用空值缓存 + 短 TTL
 * 2. 缓存雪崩：设置随机过期时间，避免集中失效
 * 3. 缓存击穿：热点文章过期，使用分布式锁重建缓存
 * 4. 数据一致性：更新文章时主动清除缓存
 * 
 * 【优化思路】
 * - 文章详情缓存：3600 秒过期时间 + 随机值防雪崩
 * - 热点文章缓存：ZSet 结构，支持动态排序
 * - 缓存 Key 规范：blog:article:{type}:{id}
 * - Redisson 提供分布式锁，防止缓存击穿
 */
@Component
@RequiredArgsConstructor
public class ArticleCache {

    private final RedissonClient redissonClient;

    @Value("${blog.cache.article-detail-ttl:3600}")
    private Integer articleDetailTtl;

    @Value("${blog.cache.hot-article-ttl:1800}")
    private Integer hotArticleTtl;

    // ==================== 文章详情缓存 ====================

    /**
     * 获取文章详情缓存
     * 
     * @param articleId 文章 ID
     * @return 文章详情对象
     */
    public Object getArticleDetail(Long articleId) {
        String key = buildArticleDetailKey(articleId);
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 设置文章详情缓存
     * 
     * @param articleId 文章 ID
     * @param articleDetail 文章详情对象
     */
    public void setArticleDetail(Long articleId, Object articleDetail) {
        String key = buildArticleDetailKey(articleId);
        RBucket<Object> bucket = redissonClient.getBucket(key);
        // 优化：添加随机时间防雪崩（±300 秒）
        int randomTtl = articleDetailTtl + ThreadLocalRandom.current().nextInt(-300, 301);
        bucket.set(articleDetail, randomTtl, TimeUnit.SECONDS);
    }

    /**
     * 删除文章详情缓存
     * 
     * @param articleId 文章 ID
     */
    public void deleteArticleDetail(Long articleId) {
        String key = buildArticleDetailKey(articleId);
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.delete();
    }

    // ==================== 热点文章缓存（ZSet） ====================

    /**
     * 添加文章到热点列表
     * 
     * @param articleId 文章 ID
     * @param score 分数（浏览量）
     */
    public void addHotArticle(Long articleId, double score) {
        String key = "blog:hot:articles";
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(key);
        // 使用 RScoredSortedSet 直接添加带分数的元素
        scoredSet.add(score, String.valueOf(articleId));
    }

    /**
     * 获取热点文章 TOP10
     * 
     * @return 文章 ID 列表（按分数降序）
     */
    public Collection<String> getHotArticles() {
        String key = "blog:hot:articles";
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(key);
        
        // 获取 TOP10，分数降序
        // RScoredSortedSet.valueRangeReversed() 返回从大到小的元素
        Collection<String> result = scoredSet.valueRangeReversed(0, 9);
        return result != null ? result : new HashSet<>();
    }

    /**
     * 更新热点文章分数
     * 
     * @param articleId 文章 ID
     * @param score 新分数
     */
    public void updateHotArticleScore(Long articleId, double score) {
        String key = "blog:hot:articles";
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(key);
        // 直接设置新分数（如果元素不存在会自动添加）
        scoredSet.add(score, String.valueOf(articleId));
    }

    /**
     * 删除热点文章
     * 
     * @param articleId 文章 ID
     */
    public void removeHotArticle(Long articleId) {
        String key = "blog:hot:articles";
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(key);
        scoredSet.remove(String.valueOf(articleId));
    }

    /**
     * 清空热点文章缓存
     */
    public void clearHotArticles() {
        String key = "blog:hot:articles";
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(key);
        scoredSet.clear();
    }

    // ==================== 文章 ID 集合缓存 ====================

    /**
     * 缓存文章 ID 集合（用于分页优化）
     * 
     * @param categoryIds 分类 ID 列表
     * @param articleIds 文章 ID 列表
     */
    public void cacheArticleIds(String categoryIds, List<Long> articleIds) {
        String key = "blog:article:ids:" + categoryIds;
        RBucket<String> bucket = redissonClient.getBucket(key);
        // 缓存 10 分钟
        bucket.set(JSON.toJSONString(articleIds), 600, TimeUnit.SECONDS);
    }

    /**
     * 获取缓存的文章 ID 集合
     * 
     * @param categoryIds 分类 ID 列表
     * @return 文章 ID 列表
     */
    public List<Long> getArticleIds(String categoryIds) {
        String key = "blog:article:ids:" + categoryIds;
        RBucket<String> bucket = redissonClient.getBucket(key);
        String json = bucket.get();
        if (json == null) {
            return null;
        }
        return JSON.parseArray(json, Long.class);
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建文章详情缓存 Key
     * 
     * @param articleId 文章 ID
     * @return 缓存 Key
     */
    private String buildArticleDetailKey(Long articleId) {
        return "blog:article:detail:" + articleId;
    }
}
