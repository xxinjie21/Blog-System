package com.blog.controller;

import com.blog.annotation.RateLimit;
import com.blog.annotation.RequiresPermission;
import com.blog.dto.CommentDTO;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章评论控制器
 */
@Tag(name = "评论接口", description = "文章评论的查看、创建、删除、点赞")
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "根据文章 ID 查询评论列表")
    @GetMapping("/article/{articleId}")
    public Result<List<CommentVO>> listByArticleId(@PathVariable Long articleId) {
        List<CommentVO> comments = commentService.listByArticleId(articleId);
        return Result.success(comments);
    }

    /**
     * 添加评论
     * 
     * @param commentDTO 评论 DTO
     * @return 评论 ID
     */
    @Operation(summary = "添加评论", description = "需要 comment:create 权限，每分钟限 10 次")
    @PostMapping
    @RequiresPermission("comment:create")
    @RateLimit(key = "comment:create", count = 10, time = 1)
    public Result<Long> addComment(@RequestBody @Validated CommentDTO commentDTO) {
        Long commentId = commentService.addComment(commentDTO);
        return Result.success(commentId);
    }

    /**
     * 删除评论
     * 
     * @param id 评论 ID
     * @return 响应结果
     */
    @Operation(summary = "删除评论", description = "需要 comment:delete 权限")
    @DeleteMapping("/{id}")
    @RequiresPermission("comment:delete")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }

    /**
     * 点赞评论
     * 
     * @param id 评论 ID
     * @return 响应结果
     */
    @Operation(summary = "点赞评论", description = "每分钟限 20 次")
    @PostMapping("/{id}/like")
    @RateLimit(key = "like", count = 20, time = 1)
    public Result<Void> likeComment(@PathVariable Long id) {
        commentService.likeComment(id);
        return Result.success();
    }
}
