package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改密码请求 DTO
 *
 * 【企业级考点】
 * 1. 新密码强制复杂度校验（与注册一致）
 * 2. 旧密码 + 新密码 + 确认密码三段校验，防误操作
 */
@Data
public class ChangePasswordDTO {

    /**
     * 旧密码
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码（复杂度：至少一个大小写字母 + 一个数字）
     */
    @NotBlank(message = "新密码不能为空")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}$",
            message = "新密码需 8-20 位，且包含大写字母、小写字母和数字")
    private String newPassword;

    /**
     * 确认新密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
