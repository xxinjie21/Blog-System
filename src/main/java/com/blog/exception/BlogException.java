package com.blog.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 博客系统业务异常类
 * 
 * 【面试考点】
 * 1. 自定义异常体系
 * 2. 全局异常处理
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BlogException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private Integer code;

    public BlogException(String message) {
        super(message);
        this.code = 500;
    }

    public BlogException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BlogException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }
}
