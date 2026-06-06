package com.blog.controller;

import com.blog.entity.Tag;
import com.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文章标签控制器
 */
@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 获取热门标签列表
     * 
     * @param limit 数量限制
     * @return 标签列表
     */
    @GetMapping("/hot")
    public Result<List<Tag>> listHotTags(@RequestParam(defaultValue = "10") Integer limit) {
        List<Tag> tags = tagService.listHotTags(limit);
        return Result.success(tags);
    }
}
