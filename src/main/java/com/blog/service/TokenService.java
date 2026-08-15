package com.blog.service;

import com.blog.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 令牌会话管理服务（Redis）
 *
 * 【企业级考点】
 * 1. JWT 本身无法撤销，撤销能力靠 Redis 会话记录实现
 * 2. 三层 Key 设计：
 *    - auth:access:{token}      单个访问令牌 → 登出/黑名单
 *    - auth:refresh:user:{id}   该用户的刷新令牌集合 → 刷新令牌轮换与回收
 *    - auth:user:{id}           用户会话标记 → 禁用账号/改密码时一键踢下线所有会话
 * 3. 禁用账号 / 修改密码 = 删 auth:user:{id}，该用户所有已签发令牌立即失效
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisUtil redisUtil;

    /** 访问令牌前缀 */
    private static final String ACCESS_PREFIX = "auth:access:";
    /** 刷新令牌（按用户集合）前缀 */
    private static final String REFRESH_USER_PREFIX = "auth:refresh:user:";
    /** 用户会话标记前缀 */
    private static final String USER_PREFIX = "auth:user:";
    /** 登录失败计数前缀 */
    private static final String FAIL_PREFIX = "auth:fail:";
    /** 登录锁定前缀 */
    private static final String LOCK_PREFIX = "auth:lock:";

    /**
     * 保存访问令牌
     */
    public void saveAccessToken(String token, Long userId, long ttlMinutes) {
        redisUtil.set(ACCESS_PREFIX + token, userId, ttlMinutes, TimeUnit.MINUTES);
    }

    /**
     * 保存刷新令牌（加入该用户的刷新令牌集合）
     */
    public void saveRefreshToken(String token, Long userId, long ttlDays) {
        String setKey = REFRESH_USER_PREFIX + userId;
        redisUtil.sadd(setKey, token);
        redisUtil.expire(setKey, ttlDays, TimeUnit.DAYS);
    }

    /**
     * 校验访问令牌是否有效（未被登出）
     */
    public boolean validateAccessToken(String token) {
        return redisUtil.hasKey(ACCESS_PREFIX + token);
    }

    /**
     * 校验刷新令牌是否有效（在集合中且未被回收）
     */
    public boolean validateRefreshToken(String token, Long userId) {
        return Boolean.TRUE.equals(redisUtil.sisMember(REFRESH_USER_PREFIX + userId, token));
    }

    /**
     * 标记用户会话有效（登录时调用）
     */
    public void markUserActive(Long userId, long ttlDays) {
        redisUtil.set(USER_PREFIX + userId, 1, ttlDays, TimeUnit.DAYS);
    }

    /**
     * 校验用户会话标记（不存在 = 账号被禁用或密码已修改）
     */
    public boolean isUserActive(Long userId) {
        return redisUtil.hasKey(USER_PREFIX + userId);
    }

    /**
     * 移除单个访问令牌（单次登出）
     */
    public void removeAccessToken(String token) {
        redisUtil.delete(ACCESS_PREFIX + token);
    }

    /**
     * 回收该用户全部刷新令牌（登出/禁用/改密时调用）
     */
    public void revokeUserRefreshTokens(Long userId) {
        redisUtil.delete(REFRESH_USER_PREFIX + userId);
    }

    /**
     * 轮换刷新令牌：删旧令牌，存新令牌
     */
    public void rotateRefreshToken(String oldToken, String newToken, Long userId, long ttlDays) {
        String setKey = REFRESH_USER_PREFIX + userId;
        redisUtil.srem(setKey, oldToken);
        redisUtil.sadd(setKey, newToken);
        redisUtil.expire(setKey, ttlDays, TimeUnit.DAYS);
    }

    /**
     * 一键踢下线该用户全部会话（禁用账号/修改密码）
     */
    public void killUserSessions(Long userId) {
        redisUtil.delete(USER_PREFIX + userId);
        redisUtil.delete(REFRESH_USER_PREFIX + userId);
    }

    // ==================== 登录防暴力破解 ====================

    /**
     * 累加登录失败次数，并设置计数过期时间（窗口期内有效）
     *
     * @return 当前失败次数
     */
    public long incrementFailCount(String username, long lockMinutes) {
        String key = FAIL_PREFIX + username;
        Long count = redisUtil.increment(key);
        redisUtil.expire(key, lockMinutes, TimeUnit.MINUTES);
        return count;
    }

    /**
     * 锁定账号（锁定时间内禁止登录）
     */
    public void lockUser(String username, long lockMinutes) {
        redisUtil.set(LOCK_PREFIX + username, 1, lockMinutes, TimeUnit.MINUTES);
    }

    /**
     * 判断账号是否被锁定
     */
    public boolean isLocked(String username) {
        return redisUtil.hasKey(LOCK_PREFIX + username);
    }

    /**
     * 登录成功清空失败计数
     */
    public void clearFailCount(String username) {
        redisUtil.delete(FAIL_PREFIX + username);
    }
}
