package com.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求 DTO
 *
 * 【企业级考点】
 * 1. 密码复杂度校验：8-20 位，必须同时包含大写字母、小写字母、数字
 * 2. 用户名白名单校验：仅字母、数字、下划线，防注入与特殊字符
 * 3. 校验在 Controller 层 @Validated 前置拦截，非法请求不进业务层
 */
@Data
public class RegisterDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度 3-20 位")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    /**
     * 密码（复杂度：至少一个大小写字母 + 一个数字）
     */
    @NotBlank(message = "密码不能为空")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}$",
            message = "密码需 8-20 位，且包含大写字母、小写字母和数字")
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
}
