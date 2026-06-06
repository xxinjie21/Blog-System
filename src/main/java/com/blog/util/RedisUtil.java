package com.blog.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * 
 * 【面试考点】
 * 1. RedisTemplate 封装
 * 2. 常用 Redis 操作
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== String 操作 ====================

    /**
     * 设置 String 值
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置 String 值（带过期时间）
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取 String 值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 自增 1
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 自增指定值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 自减 1
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    // ==================== Key 操作 ====================

    /**
     * 删除 Key
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 判断 Key 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    // ==================== Set 操作 ====================

    /**
     * 添加 Set 成员
     */
    public Long sadd(String key, Object... members) {
        return redisTemplate.opsForSet().add(key, members);
    }

    /**
     * 判断成员是否在 Set 中
     */
    public Boolean sisMember(String key, Object member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /**
     * 移除 Set 成员
     */
    public Long srem(String key, Object... members) {
        return redisTemplate.opsForSet().remove(key, members);
    }

    /**
     * 获取 Set 所有成员
     */
    public java.util.Set<Object> smembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    // ==================== ZSet 操作 ====================

    /**
     * 添加 ZSet 成员
     */
    public Boolean zadd(String key, Object value, double score) {
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 获取 ZSet 指定范围（降序）
     */
    public java.util.Set<Object> zrevRange(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 获取 ZSet 指定范围（降序，带分数）
     */
    public java.util.Set<?> zrevRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /**
     * 移除 ZSet 成员
     */
    public Long zrem(String key, Object... values) {
        return redisTemplate.opsForZSet().remove(key, values);
    }
}
