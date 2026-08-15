package com.blog.service;

import com.blog.mapper.PermissionMapper;
import com.blog.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 权限服务
 *
 * 【企业级设计】
 * 1. 用户权限列表存 Redis 缓存（auth:perm:{userId}），避免每次请求都查数据库
 * 2. 权限变更（分配/撤销角色）时清除缓存，下次请求重新加载
 * 3. 权限格式为"资源:操作"（如 article:create），拦截器做精确匹配
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;
    private final RedisUtil redisUtil;

    /** 权限缓存 Key 前缀 */
    private static final String PERM_PREFIX = "auth:perm:";
    /** 权限缓存过期时间（小时） */
    private static final long PERM_CACHE_HOURS = 24;

    /**
     * 获取用户全部权限编码（优先查缓存）
     */
    public Set<String> getUserPermissions(Long userId) {
        String key = PERM_PREFIX + userId;
        Set<String> cached = (Set<String>) redisUtil.get(key);
        if (cached != null) {
            return cached;
        }

        // 缓存未命中，查数据库
        List<String> codes = permissionMapper.selectPermissionCodesByUserId(userId);
        Set<String> permSet = new java.util.HashSet<>(codes);

        // 写入缓存
        redisUtil.set(key, permSet, PERM_CACHE_HOURS, TimeUnit.HOURS);
        return permSet;
    }

    /**
     * 判断用户是否拥有指定权限
     */
    public boolean hasPermission(Long userId, String requiredCode) {
        Set<String> permissions = getUserPermissions(userId);
        return permissions.contains(requiredCode);
    }

    /**
     * 清除用户权限缓存（权限变更后调用）
     */
    public void clearPermissionCache(Long userId) {
        redisUtil.delete(PERM_PREFIX + userId);
    }
}
