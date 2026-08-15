package com.blog.aspect;

import com.blog.annotation.RateLimit;
import com.blog.exception.BlogException;
import com.blog.util.RedisUtil;
import com.blog.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 接口限流切面
 *
 * 【企业级设计】
 * 1. 基于 Redis INCR + EXPIRE 实现滑动窗口限流（与防爆破同一原理）
 * 2. 两种限流维度：按 userId（登录用户）或按 IP（匿名接口）
 * 3. 超限返回 429 Too Many Requests
 *
 * 用法：在 Controller 方法上加 @RateLimit(key = "接口名", count = N, time = M)
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisUtil redisUtil;

    private static final String RATE_LIMIT_PREFIX = "rate:";

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 拼接 Redis key：rate:{key}:{userId 或 IP}
        String identity = getIdentity(rateLimit.byUser());
        String redisKey = RATE_LIMIT_PREFIX + rateLimit.key() + ":" + identity;

        // INCR + EXPIRE（与防爆破同一原理）
        long count = redisUtil.increment(redisKey);
        long ttlSeconds = getTtlSeconds(rateLimit);
        redisUtil.expire(redisKey, ttlSeconds, TimeUnit.SECONDS);

        // 超限判断
        if (count > rateLimit.count()) {
            log.warn("[限流] 接口={}, 身份={}, 请求数={}/{}, 限流拒绝",
                    rateLimit.key(), identity, count, rateLimit.count());
            throw new BlogException(429, "请求过于频繁，请 " + ttlSeconds + " 秒后重试");
        }

        return joinPoint.proceed();
    }

    /**
     * 获取限流身份（userId 或 IP）
     */
    private String getIdentity(boolean byUser) {
        if (byUser) {
            Long userId = UserContext.getUserId();
            return userId != null ? "u:" + userId : "anon";
        }
        return "ip:" + getClientIp();
    }

    /**
     * 计算 TTL 秒数
     */
    private long getTtlSeconds(RateLimit rateLimit) {
        return switch (rateLimit.timeUnit()) {
            case SECONDS -> rateLimit.time();
            case MINUTES -> rateLimit.time() * 60;
            case HOURS   -> rateLimit.time() * 3600;
            case DAYS    -> rateLimit.time() * 86400;
            default -> rateLimit.time() * 60;
        };
    }

    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
