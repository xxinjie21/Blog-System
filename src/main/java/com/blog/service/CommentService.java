package com.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.dto.CommentDTO;
import com.blog.entity.Comment;
import com.blog.vo.CommentVO;

import java.util.List;

/**
 * 文章评论服务接口
 */
public interface CommentService extends IService<Comment> {

    /**
     * 根据文章 ID 查询评论列表
     * 
     * @param articleId 文章 ID
     * @return 评论列表
     */
    List<CommentVO> listByArticleId(Long articleId);

    /**
     * 添加评论
     * 
     * @param commentDTO 评论 DTO
     * @return 评论 ID
     */
    Long addComment(CommentDTO commentDTO);

    /**
     * 删除评论
     * 
     * @param id 评论 ID
     */
    void deleteComment(Long id);

    /**
     * 点赞评论
     * 
     * @param commentId 评论 ID
     */
    void likeComment(Long commentId);
}
