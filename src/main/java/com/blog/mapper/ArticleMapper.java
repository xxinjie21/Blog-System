package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.ArticleQueryDTO;
import com.blog.entity.Article;
import com.blog.vo.ArticleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文章 Mapper 接口
 * 考点：MyBatis-Plus 分页查询、多条件动态 SQL
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 分页查询文章（多条件）
     * 优化：使用关联索引 idx_category_published 或 idx_create_published
     * 
     * @param page 分页对象
     * @param queryDTO 查询条件
     * @return 文章 VO 列表
     */
    IPage<ArticleVO> pageList(Page<Article> page, @Param("query") ArticleQueryDTO queryDTO);

    /**
     * 查询热点文章 TOP10
     * 优化：使用索引 idx_view_count + idx_is_published
     * 
     * @param limit 数量限制
     * @return 热点文章列表
     */
    @Select("SELECT id, title, summary, cover_image, view_count, like_count, create_time " +
            "FROM blog_article " +
            "WHERE is_published = 1 " +
            "ORDER BY view_count DESC " +
            "LIMIT #{limit}")
    List<ArticleVO> selectHotArticles(@Param("limit") Integer limit);

    /**
     * 批量更新浏览量
     * 优化：批量操作减少 DB 交互次数
     * 
     * @param viewCounts 文章 ID 和浏览量的 Map
     */
    void batchUpdateViewCounts(@Param("viewCounts") java.util.Map<Long, Integer> viewCounts);
}
