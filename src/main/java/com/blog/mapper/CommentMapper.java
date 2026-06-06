package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章评论 Mapper 接口
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 根据文章 ID 查询评论列表
     * 优化：使用复合索引 idx_article_audit_time
     * 
     * @param articleId 文章 ID
     * @return 评论列表
     */
    List<Comment> selectByArticleId(@Param("articleId") Long articleId);
}
