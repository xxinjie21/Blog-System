package com.blog.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解
 *
 * 标注在 Controller 方法上，限制同一用户/IP 在指定时间内的请求次数。
 * 示例：@RateLimit(key = "comment", count = 5, time = 1, timeUnit = TimeUnit.MINUTES)
 * 含义：同一用户每分钟最多 5 次评论请求，超限返回 429。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流 Key 前缀（用于 Redis key 区分不同接口）
     */
    String key();

    /**
     * 时间窗口内允许的最大请求数
     */
    int count() default 10;

    /**
     * 时间窗口大小
     */
    long time() default 1;

    /**
     * 时间窗口单位
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    /**
     * 限流维度：true 按用户（userId），false 按 IP
     */
    boolean byUser() default true;
}
