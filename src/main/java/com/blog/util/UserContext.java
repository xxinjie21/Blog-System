package com.blog.util;

/**
 * 当前登录用户上下文
 *
 * 【面试考点】
 * 1. ThreadLocal 原理：每个线程一个副本，请求结束必须 remove 防内存泄漏
 * 2. 作用：拦截器从 JWT 解出 userId 放进这里，Controller 直接取
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /**
     * 设置当前用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取当前用户 ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 清除当前用户（拦截器 afterCompletion 调用，防止线程池复用泄漏）
     */
    public static void clear() {
        USER_ID.remove();
    }
}
