package com.blog.cache;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 文章点赞缓存策略类（基于 Redisson）
 * 
 * 【面试考点】
 * 1. 防重复点赞：Redis Set 结构，判断是否已点赞
 * 2. 高并发写优化：Redis INCR/DECR 原子操作
 * 3. 异步持久化：定时任务同步到 MySQL
 * 4. Redisson 分布式锁：防止缓存击穿
 * 
 * 【优化思路】
 * - 点赞总数：AtomicLong，INCR/DECR 原子操作
 * - 用户点赞记录：Set 结构，防重复点赞
 * - 持久化策略：定时任务，削峰填谷
 */
@Component
@RequiredArgsConstructor
public class LikeCache {

    private final RedissonClient redissonClient;

    /**
     * 构建点赞总数 Key
     * 
     * @param articleId 文章 ID
     * @return 缓存 Key
     */
    private String buildLikeCountKey(Long articleId) {
        return "blog:article:like:" + articleId;
    }

    /**
     * 构建用户点赞记录 Key
     * 
     * @param articleId 文章 ID
     * @return 缓存 Key
     */
    private String buildUserLikeKey(Long articleId) {
        return "blog:article:like:users:" + articleId;
    }

    /**
     * 点赞文章
     * 
     * 【优化点】
     * 1. Set 判断是否已点赞，防止重复
     * 2. AtomicLong 原子操作，线程安全
     * 
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return true-点赞成功，false-已点赞过
     */
    public boolean likeArticle(Long articleId, String userId) {
        String likeCountKey = buildLikeCountKey(articleId);
        String userLikeKey = buildUserLikeKey(articleId);

        // 使用分布式锁保证原子性
        String lockKey = "lock:like:" + articleId + ":" + userId;
        org.redisson.api.RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock(5, java.util.concurrent.TimeUnit.SECONDS);

            // 检查是否已点赞
            RSet<String> userLikeSet = redissonClient.getSet(userLikeKey);
            if (userLikeSet.contains(userId)) {
                return false;
            }

            // 点赞数 +1
            RAtomicLong likeCount = redissonClient.getAtomicLong(likeCountKey);
            likeCount.incrementAndGet();
            
            // 记录用户点赞
            userLikeSet.add(userId);

            return true;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 取消点赞文章
     * 
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return true-取消成功，false-未点赞过
     */
    public boolean unlikeArticle(Long articleId, String userId) {
        String likeCountKey = buildLikeCountKey(articleId);
        String userLikeKey = buildUserLikeKey(articleId);

        String lockKey = "lock:like:" + articleId + ":" + userId;
        org.redisson.api.RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock(5, java.util.concurrent.TimeUnit.SECONDS);

            RSet<String> userLikeSet = redissonClient.getSet(userLikeKey);
            if (!userLikeSet.contains(userId)) {
                return false;
            }

            RAtomicLong likeCount = redissonClient.getAtomicLong(likeCountKey);
            likeCount.decrementAndGet();
            
            userLikeSet.remove(userId);

            return true;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 获取文章点赞数
     * 
     * @param articleId 文章 ID
     * @return 点赞数
     */
    public Long getLikeCount(Long articleId) {
        String key = buildLikeCountKey(articleId);
        RAtomicLong likeCount = redissonClient.getAtomicLong(key);
        Long value = likeCount.get();
        return value != null ? value : 0L;
    }

    /**
     * 检查用户是否已点赞
     * 
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return true-已点赞，false-未点赞
     */
    public boolean isLiked(Long articleId, String userId) {
        String key = buildUserLikeKey(articleId);
        RSet<String> userLikeSet = redissonClient.getSet(key);
        return userLikeSet.contains(userId);
    }

    /**
     * 获取点赞用户列表
     * 
     * @param articleId 文章 ID
     * @return 用户 ID 集合
     */
    public Set<String> getLikeUsers(Long articleId) {
        String key = buildUserLikeKey(articleId);
        RSet<String> userLikeSet = redissonClient.getSet(key);
        Set<String> members = userLikeSet.readAll();
        return members != null ? members : new HashSet<>();
    }

    /**
     * 设置点赞数（初始化或恢复）
     * 
     * @param articleId 文章 ID
     * @param count 点赞数
     */
    public void setLikeCount(Long articleId, Long count) {
        String key = buildLikeCountKey(articleId);
        RAtomicLong likeCount = redissonClient.getAtomicLong(key);
        likeCount.set(count);
    }

    /**
     * 删除点赞缓存
     * 
     * @param articleId 文章 ID
     */
    public void deleteLikeCache(Long articleId) {
        String likeCountKey = buildLikeCountKey(articleId);
        String userLikeKey = buildUserLikeKey(articleId);
        
        RBucket<Object> likeCountBucket = redissonClient.getBucket(likeCountKey);
        RBucket<Object> userLikeBucket = redissonClient.getBucket(userLikeKey);
        
        likeCountBucket.delete();
        userLikeBucket.delete();
    }

    /**
     * 获取所有点赞缓存数据
     * 
     * 【优化点】
     * 1. 定时任务调用，持久化到 MySQL
     * 2. 批量更新，减少 DB 交互
     * 
     * @return 文章 ID -> 点赞数 Map
     */
    public Map<Long, Long> getAllLikeCounts() {
        Map<Long, Long> likeCounts = new HashMap<>();
        
        // 获取所有点赞计数的 key
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern("blog:article:like:*");
        for (String key : keys) {
            // 排除 users 后缀的 Key
            if (key.contains(":users:")) {
                continue;
            }
            
            try {
                // 提取 articleId
                String articleIdStr = key.replace("blog:article:like:", "");
                Long articleId = Long.valueOf(articleIdStr);
                Long count = getLikeCount(articleId);
                if (count != null && count > 0) {
                    likeCounts.put(articleId, count);
                }
            } catch (NumberFormatException e) {
                // 跳过无效的 key
            }
        }
        
        return likeCounts;
    }
}
