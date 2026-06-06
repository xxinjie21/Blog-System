package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleQueryDTO;
import com.blog.entity.Article;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleVO;
import com.blog.vo.HotArticleVO;

import java.util.List;

/**
 * 文章服务接口
 */
public interface ArticleService extends IService<Article> {

    /**
     * 分页查询文章列表
     * 
     * @param queryDTO 查询条件
     * @return 文章分页列表
     */
    IPage<ArticleVO> pageList(ArticleQueryDTO queryDTO);

    /**
     * 获取文章详情
     * 
     * @param id 文章 ID
     * @return 文章详情
     */
    ArticleDetailVO getDetail(Long id);

    /**
     * 发布文章
     * 
     * @param articleDTO 文章 DTO
     * @return 文章 ID
     */
    Long publish(ArticleDTO articleDTO);

    /**
     * 更新文章
     * 
     * @param articleDTO 文章 DTO
     */
    void update(ArticleDTO articleDTO);

    /**
     * 删除文章
     * 
     * @param id 文章 ID
     */
    void delete(Long id);

    /**
     * 获取热点文章 TOP10
     * 
     * @return 热点文章列表
     */
    List<HotArticleVO> getHotArticles();

    /**
     * 增加文章浏览量
     * 
     * @param articleId 文章 ID
     */
    void incrementViewCount(Long articleId);

    /**
     * 点赞文章
     * 
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return 是否点赞成功
     */
    boolean likeArticle(Long articleId, String userId);

    /**
     * 取消点赞
     * 
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return 是否取消成功
     */
    boolean unlikeArticle(Long articleId, String userId);
}
