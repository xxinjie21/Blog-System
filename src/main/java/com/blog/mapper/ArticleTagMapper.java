package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章 - 标签关联 Mapper 接口
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {

    /**
     * 批量插入文章标签关联
     * 
     * @param articleId 文章 ID
     * @param tagIds 标签 ID 列表
     * @return 影响行数
     */
    int batchInsert(@Param("articleId") Long articleId, @Param("tagIds") List<Long> tagIds);

    /**
     * 根据文章 ID 删除关联关系
     * 
     * @param articleId 文章 ID
     * @return 影响行数
     */
    int deleteByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据文章 ID 查询标签 ID 列表
     * 
     * @param articleId 文章 ID
     * @return 标签 ID 列表
     */
    List<Long> selectTagIdsByArticleId(@Param("articleId") Long articleId);
}
