package com.blog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章查询数据传输对象
 * 考点：多条件查询参数封装
 */
@Data
public class ArticleQueryDTO {

    /**
     * 文章标题（模糊查询）
     * 优化：使用 idx_title 索引
     */
    private String title;

    /**
     * 分类 ID
     * 优化：使用 idx_category_id 索引
     */
    private Long categoryId;

    /**
     * 作者（模糊查询）
     * 优化：使用 idx_author 索引
     */
    private String author;

    /**
     * 是否发布 0-草稿 1-发布
     * 优化：使用 idx_is_published 索引
     */
    private Integer isPublished;

    /**
     * 是否置顶
     */
    private Integer isTop;

    /**
     * 开始时间
     * 优化：使用 idx_create_time 索引
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
