package com.blog.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 评论数据传输对象
 * 考点：参数校验、邮箱格式验证
 */
@Data
public class CommentDTO {

    /**
     * 文章 ID
     */
    @NotNull(message = "文章 ID 不能为空")
    private Long articleId;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过 50 个字符")
    private String userName;

    /**
     * 用户邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String userEmail;

    /**
     * 评论内容
     */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容长度不能超过 1000 个字符")
    private String content;

    /**
     * 父评论 ID（0-顶级评论）
     */
    private Long parentId;
}
