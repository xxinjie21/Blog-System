package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章标签实体类
 * 考点：唯一索引、热门标签查询优化
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_tag")
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标签 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 标签名称 - 唯一索引 uk_name
     */
    private String name;

    /**
     * 标签 Slug - 唯一索引 uk_slug
     */
    private String slug;

    /**
     * 文章数量（冗余字段，减少 COUNT 查询）
     * 优化：索引 idx_article_count 支持热门标签查询
     */
    private Integer articleCount;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
