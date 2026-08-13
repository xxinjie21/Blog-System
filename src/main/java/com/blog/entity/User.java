package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类（JWT 登录）
 *
 * 【面试考点】
 * 1. BCrypt 密码加密存储，绝不存明文
 * 2. status 字段：0-禁用 1-启用，登录时校验
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID - 主键自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名 - 唯一索引 uk_username
     */
    private String username;

    /**
     * 密码 - BCrypt 加密后的值
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱 - 唯一索引 uk_email
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 状态 0-禁用 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除字段
     */
    @TableLogic
    private Integer deleted;
}
