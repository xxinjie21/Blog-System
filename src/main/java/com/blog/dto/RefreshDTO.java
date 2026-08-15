package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求 DTO
 *
 * 【企业级考点】
 * 刷新令牌过期即 401，需重新登录——这是安全边界，不能无限续期
 */
@Data
public class RefreshDTO {

    /**
     * 刷新令牌
     */
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
