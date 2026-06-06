package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章分类实体类
 * 考点：唯一索引防止重复、Slug URL 友好设计
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称 - 唯一索引 uk_name
     */
    private String name;

    /**
     * 分类 Slug - 唯一索引 uk_slug（URL 友好）
     */
    private String slug;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序权重 - 索引 idx_sort_order
     */
    private Integer sortOrder;

    /**
     * 文章数量（冗余字段，减少 COUNT 查询）
     */
    private Integer articleCount;

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
}
