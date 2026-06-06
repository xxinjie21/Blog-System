package com.blog.vo;

import lombok.Data;

/**
 * 热点文章视图对象
 * 考点：精简字段、减少网络传输
 */
@Data
public class HotArticleVO {

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
     */
    private Integer viewCount;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 创建时间
     */
    private String createTime;
}
