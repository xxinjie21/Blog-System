package com.blog.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 *
 * 【企业级设计】
 * 1. 标注在 Controller 方法上，声明该接口需要的权限编码
 * 2. 拦截器通过反射读取此注解，做权限校验
 * 3. 不标 = 不检查（默认放行），标了 = 必须拥有该权限
 *
 * 示例：@RequiresPermission("article:create")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    /**
     * 需要的权限编码（资源:操作 格式）
     */
    String value();
}
