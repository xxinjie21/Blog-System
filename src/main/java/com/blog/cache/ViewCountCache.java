package com.blog.cache;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 文章浏览量缓存策略类（基于 Redisson）
 * 
 * 【面试考点】
 * 1. 高并发写优化：Redis INCR 原子操作，避免 DB 行锁竞争
 * 2. 异步持久化：定时任务批量同步到 MySQL，减少 DB 压力
 * 3. 崩溃恢复：DB 基础值 + Redis 增量值
 * 4. 原子性保证：Redisson 提供线程安全的原子操作
 * 
 * 【优化思路】
 * - 浏览量存储：AtomicLong，INCR 原子递增
 * - 持久化策略：每 5 分钟同步一次增量数据
 * - 读取策略：DB 基础值 + Redis 增量值 = 实际浏览量
 */
@Component
@RequiredArgsConstructor
public class ViewCountCache {

    private final RedissonClient redissonClient;

    /**
     * 获取浏览量缓存 Key
     * 
     * @param articleId 文章 ID
     * @return 缓存 Key
     */
    private String buildKey(Long articleId) {
        return "blog:article:view:" + articleId;
    }

    /**
     * 增加文章浏览量
     * 
     * 【优化点】
     * 1. Redisson AtomicLong 原子操作，线程安全
     * 2. 高性能：单 QPS 可达 5000+
     * 
     * @param articleId 文章 ID
     * @return 新的浏览量
     */
    public Long incrementViewCount(Long articleId) {
        String key = buildKey(articleId);
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        return atomicLong.incrementAndGet();
    }

    /**
     * 获取文章浏览量
     * 
     * @param articleId 文章 ID
     * @return 浏览量（Redis 增量值）
     */
    public Long getViewCount(Long articleId) {
        String key = buildKey(articleId);
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        Long count = atomicLong.get();
        return count != null ? count : 0L;
    }

    /**
     * 批量获取文章浏览量
     * 
     * @param articleIds 文章 ID 列表
     * @return 浏览量 Map
     */
    public Map<Long, Long> batchGetViewCounts(java.util.List<Long> articleIds) {
        Map<Long, Long> viewCounts = new HashMap<>();
        for (Long articleId : articleIds) {
            viewCounts.put(articleId, getViewCount(articleId));
        }
        return viewCounts;
    }

    /**
     * 设置浏览量（初始化或恢复）
     * 
     * @param articleId 文章 ID
     * @param count 浏览量
     */
    public void setViewCount(Long articleId, Long count) {
        String key = buildKey(articleId);
        RAtomicLong atomicLong = redissonClient.getAtomicLong(key);
        atomicLong.set(count);
    }

    /**
     * 获取所有浏览量缓存
     * 
     * 【优化点】
     * 1. 使用 Redisson 的 keys 模式遍历
     * 2. 定时任务每 5 分钟调用一次，持久化到 MySQL
     * 
     * @return 文章 ID -> 浏览量 Map
     */
    public Map<Long, Long> getAllViewCounts() {
        Map<Long, Long> viewCounts = new HashMap<>();
        
        // 获取所有匹配的 key
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("blog:article:view:*");
        for (String key : keys) {
            // 提取 articleId
            String articleIdStr = key.replace("blog:article:view:", "");
            try {
                Long articleId = Long.valueOf(articleIdStr);
                Long count = getViewCount(articleId);
                if (count != null && count > 0) {
                    viewCounts.put(articleId, count);
                }
            } catch (NumberFormatException e) {
                // 跳过无效的 key
            }
        }
        
        return viewCounts;
    }

    /**
     * 删除文章浏览量缓存
     * 
     * @param articleId 文章 ID
     */
    public void deleteViewCount(Long articleId) {
        String key = buildKey(articleId);
        RBucket<Object> bucket = redissonClient.getBucket(key);
        bucket.delete();
    }

    /**
     * 获取浏览量缓存 Key 集合
     * 
     * @return Key 集合
     */
    public Set<String> getAllViewCountKeys() {
        Set<String> keys = new HashSet<>();
        Iterable<String> keyIter = redissonClient.getKeys().getKeysByPattern("blog:article:view:*");
        for (String key : keyIter) {
            keys.add(key);
        }
        return keys;
    }
}
