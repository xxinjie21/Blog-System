package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.annotation.RequiresPermission;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleQueryDTO;
import com.blog.service.ArticleService;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleVO;
import com.blog.vo.HotArticleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章管理控制器
 * 
 * 【面试考点】
 * 1. RESTful 接口设计规范
 * 2. 参数校验 @Validated
 * 3. 统一响应格式
 */
@Tag(name = "文章接口", description = "文章的增删改查、热点文章、浏览量、点赞")
@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 分页查询文章列表
     * 
     * @param queryDTO 查询条件
     * @return 文章分页列表
     */
    @Operation(summary = "分页查询文章列表", description = "支持按分类、关键词、作者筛选")
    @GetMapping("/page")
    public Result<IPage<ArticleVO>> pageList(@ModelAttribute ArticleQueryDTO queryDTO) {
        IPage<ArticleVO> page = articleService.pageList(queryDTO);
        return Result.success(page);
    }

    /**
     * 获取文章详情
     * 
     * @param id 文章 ID
     * @return 文章详情
     */
    @Operation(summary = "获取文章详情", description = "根据文章 ID 获取完整信息")
    @GetMapping("/{id}")
    public Result<ArticleDetailVO> getDetail(@PathVariable Long id) {
        ArticleDetailVO detail = articleService.getDetail(id);
        return Result.success(detail);
    }

    /**
     * 发布文章
     * 
     * @param articleDTO 文章 DTO
     * @return 文章 ID
     */
    @Operation(summary = "发布文章", description = "需要 article:create 权限")
    @PostMapping
    @RequiresPermission("article:create")
    public Result<Long> publish(@RequestBody @Validated ArticleDTO articleDTO) {
        Long articleId = articleService.publish(articleDTO);
        return Result.success(articleId);
    }

    /**
     * 更新文章
     * 
     * @param articleDTO 文章 DTO
     * @return 响应结果
     */
    @Operation(summary = "更新文章", description = "需要 article:update 权限")
    @PutMapping
    @RequiresPermission("article:update")
    public Result<Void> update(@RequestBody @Validated ArticleDTO articleDTO) {
        articleService.update(articleDTO);
        return Result.success();
    }

    /**
     * 删除文章
     * 
     * @param id 文章 ID
     * @return 响应结果
     */
    @Operation(summary = "删除文章", description = "需要 article:delete 权限")
    @DeleteMapping("/{id}")
    @RequiresPermission("article:delete")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return Result.success();
    }

    /**
     * 获取热点文章 TOP10
     * 
     * @return 热点文章列表
     */
    @Operation(summary = "获取热点文章 TOP10", description = "按浏览量排序")
    @GetMapping("/hot")
    public Result<List<HotArticleVO>> getHotArticles() {
        List<HotArticleVO> hotArticles = articleService.getHotArticles();
        return Result.success(hotArticles);
    }

    /**
     * 增加文章浏览量
     * 
     * @param id 文章 ID
     * @return 响应结果
     */
    @Operation(summary = "增加文章浏览量", description = "文章详情页加载时调用")
    @PostMapping("/{id}/view")
    public Result<Void> incrementViewCount(@PathVariable Long id) {
        articleService.incrementViewCount(id);
        return Result.success();
    }

    /**
     * 点赞文章
     * 
     * @param id 文章 ID
     * @param userId 用户 ID（实际项目中从 Token 获取）
     * @return 响应结果
     */
    @Operation(summary = "点赞文章", description = "基于 Redis Set 实现，防重复点赞")
    @PostMapping("/{id}/like")
    public Result<Boolean> likeArticle(@PathVariable Long id, 
                                       @RequestParam String userId) {
        boolean success = articleService.likeArticle(id, userId);
        return Result.success(success);
    }

    /**
     * 取消点赞
     * 
     * @param id 文章 ID
     * @param userId 用户 ID
     * @return 响应结果
     */
    @Operation(summary = "取消点赞", description = "移除用户的点赞记录")
    @DeleteMapping("/{id}/like")
    public Result<Boolean> unlikeArticle(@PathVariable Long id,
                                         @RequestParam String userId) {
        boolean success = articleService.unlikeArticle(id, userId);
        return Result.success(success);
    }
}
