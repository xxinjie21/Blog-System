package com.blog.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 文章数据传输对象
 * 考点：参数校验注解使用
 */
@Data
public class ArticleDTO {

    /**
     * 文章 ID（编辑时必填）
     */
    private Long id;

    /**
     * 文章标题
     * 校验：非空、长度 1-200
     */
    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "文章标题长度不能超过 200 个字符")
    private String title;

    /**
     * 文章摘要
     */
    @Size(max = 500, message = "文章摘要长度不能超过 500 个字符")
    private String summary;

    /**
     * 文章内容
     * 校验：非空
     */
    @NotBlank(message = "文章内容不能为空")
    private String content;

    /**
     * 封面图片 URL
     */
    private String coverImage;

    /**
     * 分类 ID
     * 校验：非空
     */
    @NotNull(message = "文章分类不能为空")
    private Long categoryId;

    /**
     * 作者
     */
    private String author;

    /**
     * 标签 ID 列表
     */
    private List<Long> tagIds;

    /**
     * 是否发布 0-草稿 1-发布
     */
    private Integer isPublished;

    /**
     * 是否置顶 0-否 1-是
     */
    private Integer isTop;
}
