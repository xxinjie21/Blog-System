package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 *
 * 【面试考点】
 * 1. @NotBlank 参数校验：不接收空值
 * 2. DTO 只传输入，不把密码以外的敏感数据暴露
 */
@Data
public class LoginDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
