package com.blog.controller;

import com.blog.entity.Category;
import com.blog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文章分类控制器
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取所有分类列表
     * 
     * @return 分类列表
     */
    @GetMapping("/list")
    public Result<List<Category>> listAll() {
        List<Category> categories = categoryService.listAll();
        return Result.success(categories);
    }
}
