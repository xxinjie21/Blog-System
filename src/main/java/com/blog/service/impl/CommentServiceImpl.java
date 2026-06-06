package com.blog.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.dto.CommentDTO;
import com.blog.entity.Comment;
import com.blog.exception.BlogException;
import com.blog.mapper.CommentMapper;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章评论服务实现类
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final CommentMapper commentMapper;

    @Override
    public List<CommentVO> listByArticleId(Long articleId) {
        // 查询文章评论（已审核的）
        List<Comment> comments = commentMapper.selectByArticleId(articleId);
        
        // 只查询顶级评论
        List<Comment> topComments = comments.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());
        
        // 构建评论 VO 列表
        List<CommentVO> commentVOList = topComments.stream()
                .map(this::buildCommentVO)
                .collect(Collectors.toList());
        
        // 为每个顶级评论添加回复列表
        for (CommentVO commentVO : commentVOList) {
            List<CommentVO> replies = comments.stream()
                    .filter(c -> commentVO.getId().equals(c.getParentId()))
                    .map(this::buildCommentVO)
                    .collect(Collectors.toList());
            commentVO.setReplies(replies);
        }
        
        return commentVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(CommentDTO commentDTO) {
        Comment comment = new Comment();
        BeanUtils.copyProperties(commentDTO, comment);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        comment.setIsAudit(1); // 默认审核通过，可改为待审核
        comment.setLikeCount(0);
        
        save(comment);
        
        // TODO：更新文章的评论数
        // articleMapper.incrementCommentCount(commentDTO.getArticleId());
        
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        Comment comment = getById(id);
        if (comment == null) {
            throw new BlogException("评论不存在");
        }
        
        removeById(id);
        
        // TODO：更新文章的评论数
        // articleMapper.decrementCommentCount(comment.getArticleId());
    }

    @Override
    public void likeComment(Long commentId) {
        Comment comment = getById(commentId);
        if (comment == null) {
            throw new BlogException("评论不存在");
        }
        
        // 点赞数 +1
        comment.setLikeCount(comment.getLikeCount() + 1);
        updateById(comment);
    }

    /**
     * 构建评论 VO
     */
    private CommentVO buildCommentVO(Comment comment) {
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(comment, commentVO);
        return commentVO;
    }
}
