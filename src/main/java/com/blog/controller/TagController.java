package com.blog.controller;

import com.blog.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "获取热门标签列表", description = "按文章数排序，默认取前 10")
    @GetMapping("/hot")
    public Result<List<com.blog.entity.Tag>> listHotTags(@RequestParam(defaultValue = "10") Integer limit) {
        List<com.blog.entity.Tag> tags = tagService.listHotTags(limit);
        return Result.success(tags);
    }
}
