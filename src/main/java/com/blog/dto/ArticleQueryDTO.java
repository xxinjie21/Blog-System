package com.blog.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * 文章查询数据传输对象
 */
@Data
public class ArticleQueryDTO {

    private String title;

    private Long categoryId;

    private String author;

    private Integer isPublished;

    private Integer isTop;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 10;
}
