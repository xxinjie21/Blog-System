package com.blog.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章视图对象
 * 考点：JSON 格式化、日期时区处理
 */
@Data
public class ArticleVO {

    /**
     * 文章 ID
     */
    private Long id;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 封面图片 URL
     */
    private String coverImage;

    /**
     * 浏览量
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
     * 是否置顶
     */
    private Integer isTop;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 作者
     */
    private String author;

    /**
     * 标签 ID 列表
     */
    private java.util.List<Long> tagIds;
}
