package com.blog.vo;

import com.blog.annotation.Sensitive;
import com.blog.annotation.SensitiveType;
import lombok.Data;

/**
 * 用户信息 VO（API 返回用，脱敏处理）
 *
 * 【企业级考点】
 * 1. 实体（Entity）和 VO 分离：Entity 包含密码等敏感字段，VO 脱敏后返回前端
 * 2. 密码字段不放在 VO 里（永远不返回密码）
 * 3. 手机号、邮箱等字段用 @Sensitive 注解自动脱敏
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    @Sensitive(SensitiveType.EMAIL)
    private String email;

    @Sensitive(SensitiveType.PHONE)
    private String phone;

    private String avatar;
}
