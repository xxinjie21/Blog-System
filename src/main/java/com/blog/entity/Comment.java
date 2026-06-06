package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章评论实体类
 * 考点：复合索引优化、楼中楼回复设计
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_comment")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章 ID - 索引 idx_article_id
     */
    private Long articleId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户邮箱 - 索引 idx_user_email
     */
    private String userEmail;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论 ID（0-顶级评论）- 索引 idx_parent_id
     */
    private Long parentId;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 是否审核 0-待审核 1-通过 2-拒绝
     */
    private Integer isAudit;

    /**
     * 评论 IP
     */
    private String ipAddress;

    /**
     * 创建时间 - 索引 idx_create_time
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
