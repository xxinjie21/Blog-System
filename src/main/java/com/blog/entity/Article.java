package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章实体类
 * 考点：MyBatis-Plus 注解使用、逻辑删除、自动填充
 * 
 * @TableName 指定表名
 * @TableId 主键策略（自增）
 * @TableField 字段填充策略（自动填充创建时间、更新时间）
 * @TableLogic 逻辑删除
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_article")
public class Article implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文章 ID - 主键自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文章标题 - 建立索引 idx_title
     */
    private String title;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 文章内容
     */
    private String content;

    /**
     * 封面图片 URL
     */
    private String coverImage;

    /**
     * 分类 ID - 建立索引 idx_category_id
     */
    private Long categoryId;

    /**
     * 作者 - 建立索引 idx_author
     */
    private String author;

    /**
     * 浏览量 - 建立索引 idx_view_count
     * 优化：实际值 = DB 基础值 + Redis 增量值
     */
    private Integer viewCount;

    /**
     * 点赞数
     * 优化：实际值 = DB 基础值 + Redis 增量值
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 是否置顶 0-否 1-是
     */
    private Integer isTop;

    /**
     * 是否发布 0-草稿 1-发布 - 建立索引 idx_is_published
     */
    private Integer isPublished;

    /**
     * 创建时间 - 建立索引 idx_create_time
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
