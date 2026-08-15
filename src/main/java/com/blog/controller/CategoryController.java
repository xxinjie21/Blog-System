package com.blog.controller;

import com.blog.entity.Category;
import com.blog.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分类接口", description = "文章分类查询")
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "获取所有分类列表", description = "返回按 sort_order 排序的分类列表")
    @GetMapping("/list")
    public Result<List<Category>> listAll() {
        List<Category> categories = categoryService.listAll();
        return Result.success(categories);
    }
}
