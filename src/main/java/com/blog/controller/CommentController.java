package com.blog.controller;

import com.blog.dto.CommentDTO;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章评论控制器
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 根据文章 ID 查询评论列表
     * 
     * @param articleId 文章 ID
     * @return 评论列表
     */
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
    @PostMapping
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
    @DeleteMapping("/{id}")
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
    @PostMapping("/{id}/like")
    public Result<Void> likeComment(@PathVariable Long id) {
        commentService.likeComment(id);
        return Result.success();
    }
}
