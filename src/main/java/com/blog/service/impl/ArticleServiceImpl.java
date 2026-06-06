package com.blog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.cache.ArticleCache;
import com.blog.cache.LikeCache;
import com.blog.cache.ViewCountCache;
import com.blog.dto.ArticleDTO;
import com.blog.dto.ArticleQueryDTO;
import com.blog.entity.Article;
import com.blog.entity.ArticleTag;
import com.blog.entity.Category;
import com.blog.exception.BlogException;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.service.ArticleService;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleVO;
import com.blog.vo.HotArticleVO;
import com.blog.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章服务实现类
 * 
 * 【面试考点】
 * 1. 缓存更新策略：更新文章时主动清除缓存
 * 2. 数据一致性：DB + Redis 双写一致性保证
 * 3. 事务管理：@Transactional 保证数据一致性
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final ArticleCache articleCache;
    private final ViewCountCache viewCountCache;
    private final LikeCache likeCache;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public IPage<ArticleVO> pageList(ArticleQueryDTO queryDTO) {
        // 创建分页对象
        Page<Article> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        
        // 执行分页查询
        IPage<ArticleVO> result = articleMapper.pageList(page, queryDTO);
        
        // 批量获取 Redis 浏览量（优化：减少 Redis 请求次数）
        List<Long> articleIds = result.getRecords().stream()
                .map(ArticleVO::getId)
                .collect(Collectors.toList());
        
        if (!articleIds.isEmpty()) {
            Map<Long, Long> viewCounts = viewCountCache.batchGetViewCounts(articleIds);
            Map<Long, Long> likeCounts = new HashMap<>();
            
            // 批量获取点赞数
            for (Long id : articleIds) {
                likeCounts.put(id, likeCache.getLikeCount(id));
            }
            
            // 更新浏览量和点赞数（DB 基础值 + Redis 增量值）
            for (ArticleVO articleVO : result.getRecords()) {
                Long redisViewCount = viewCounts.get(articleVO.getId());
                if (redisViewCount != null && redisViewCount > 0) {
                    int dbViewCount = articleVO.getViewCount() != null ? articleVO.getViewCount() : 0;
                    articleVO.setViewCount(dbViewCount + redisViewCount.intValue());
                }
                
                Long redisLikeCount = likeCounts.get(articleVO.getId());
                if (redisLikeCount != null && redisLikeCount > 0) {
                    int dbLikeCount = articleVO.getLikeCount() != null ? articleVO.getLikeCount() : 0;
                    articleVO.setLikeCount(dbLikeCount + redisLikeCount.intValue());
                }
            }
        }
        
        return result;
    }

    @Override
    public ArticleDetailVO getDetail(Long id) {
        // 1. 先查缓存
        Object cachedDetail = articleCache.getArticleDetail(id);
        if (cachedDetail != null) {
            return (ArticleDetailVO) cachedDetail;
        }

        // 2. 缓存未命中，查数据库
        Article article = getById(id);
        if (article == null) {
            throw new BlogException("文章不存在");
        }

        // 3. 构建详情对象
        ArticleDetailVO detailVO = buildArticleDetailVO(article);

        // 4. 写入缓存（带过期时间）
        articleCache.setArticleDetail(id, detailVO);

        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(ArticleDTO articleDTO) {
        // 1. 构建文章实体
        Article article = new Article();
        BeanUtils.copyProperties(articleDTO, article);
        article.setCreateTime(LocalDateTime.now());
        article.setUpdateTime(LocalDateTime.now());
        article.setIsPublished(articleDTO.getIsPublished() == null ? 1 : articleDTO.getIsPublished());
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);

        // 2. 插入文章
        save(article);

        // 3. 绑定标签
        if (articleDTO.getTagIds() != null && !articleDTO.getTagIds().isEmpty()) {
            articleTagMapper.batchInsert(article.getId(), articleDTO.getTagIds());
        }

        // 4. 清除热点文章缓存（触发重新计算）
        articleCache.clearHotArticles();

        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(ArticleDTO articleDTO) {
        if (articleDTO.getId() == null) {
            throw new BlogException("文章 ID 不能为空");
        }

        // 1. 更新文章
        Article article = getById(articleDTO.getId());
        if (article == null) {
            throw new BlogException("文章不存在");
        }

        BeanUtils.copyProperties(articleDTO, article, "id", "createTime", "updateTime");
        article.setUpdateTime(LocalDateTime.now());
        updateById(article);

        // 2. 更新标签关联（先删除后插入）
        articleTagMapper.deleteByArticleId(article.getId());
        if (articleDTO.getTagIds() != null && !articleDTO.getTagIds().isEmpty()) {
            articleTagMapper.batchInsert(article.getId(), articleDTO.getTagIds());
        }

        // 3. 清除文章详情缓存（保证数据一致性）
        articleCache.deleteArticleDetail(article.getId());

        // 4. 清除热点文章缓存（触发重新计算）
        articleCache.clearHotArticles();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 1. 删除文章（逻辑删除）
        removeById(id);

        // 2. 删除标签关联
        articleTagMapper.deleteByArticleId(id);

        // 3. 清除缓存
        articleCache.deleteArticleDetail(id);
        viewCountCache.deleteViewCount(id);
        likeCache.deleteLikeCache(id);

        // 4. 清除热点文章缓存
        articleCache.clearHotArticles();
    }

    @Override
    public List<HotArticleVO> getHotArticles() {
        // 1. 从 Redis 获取热点文章 ID
        Collection<String> hotArticleIds = articleCache.getHotArticles();
        
        if (hotArticleIds != null && !hotArticleIds.isEmpty()) {
            // 2. 批量查询文章详情
            List<Long> ids = hotArticleIds.stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            
            List<Article> articles = listByIds(ids);
            if (articles != null && !articles.isEmpty()) {
                // 3. 构建 VO 列表
                return articles.stream()
                        .map(this::buildHotArticleVO)
                        .collect(Collectors.toList());
            }
        }

        // 4. 缓存未命中，从数据库查询并重建缓存
        List<ArticleVO> hotArticles = articleMapper.selectHotArticles(10);
        if (hotArticles != null && !hotArticles.isEmpty()) {
            // 5. 写入 Redis 缓存
            for (ArticleVO articleVO : hotArticles) {
                articleCache.addHotArticle(articleVO.getId(), articleVO.getViewCount());
            }

            return hotArticles.stream()
                    .map(this::buildHotArticleVO)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public void incrementViewCount(Long articleId) {
        // Redis 原子操作增加浏览量
        viewCountCache.incrementViewCount(articleId);
        
        // 更新热点文章分数
        Long newViewCount = viewCountCache.getViewCount(articleId);
        articleCache.updateHotArticleScore(articleId, newViewCount);
    }

    @Override
    public boolean likeArticle(Long articleId, String userId) {
        // 点赞
        boolean success = likeCache.likeArticle(articleId, userId);
        
        if (success) {
            // 更新文章点赞数（DB 基础值）
            Article article = getById(articleId);
            if (article != null) {
                // 注意：这里只更新 DB 基础值，实际显示值 = DB 基础值 + Redis 增量值
                // 为了简化，实际项目中应该使用异步消息队列更新
            }
        }
        
        return success;
    }

    @Override
    public boolean unlikeArticle(Long articleId, String userId) {
        return likeCache.unlikeArticle(articleId, userId);
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建文章详情 VO
     */
    private ArticleDetailVO buildArticleDetailVO(Article article) {
        ArticleDetailVO detailVO = new ArticleDetailVO();
        BeanUtils.copyProperties(article, detailVO);

        // 获取分类名称
        if (article.getCategoryId() != null) {
            Category category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                detailVO.setCategoryName(category.getName());
            }
        }

        // 获取标签列表
        List<Long> tagIds = articleTagMapper.selectTagIdsByArticleId(article.getId());
        if (tagIds != null && !tagIds.isEmpty()) {
            List<TagVO> tags = tagIds.stream()
                    .map(tagId -> {
                        TagVO tagVO = new TagVO();
                        tagVO.setId(tagId);
                        // 这里可以查询标签详情
                        return tagVO;
                    })
                    .collect(Collectors.toList());
            detailVO.setTags(tags);
        }

        // 获取实际浏览量和点赞数（DB 基础值 + Redis 增量值）
        Long redisViewCount = viewCountCache.getViewCount(article.getId());
        if (redisViewCount != null && redisViewCount > 0) {
            int dbViewCount = detailVO.getViewCount() != null ? detailVO.getViewCount() : 0;
            detailVO.setViewCount(dbViewCount + redisViewCount.intValue());
        }

        Long redisLikeCount = likeCache.getLikeCount(article.getId());
        if (redisLikeCount != null && redisLikeCount > 0) {
            int dbLikeCount = detailVO.getLikeCount() != null ? detailVO.getLikeCount() : 0;
            detailVO.setLikeCount(dbLikeCount + redisLikeCount.intValue());
        }

        return detailVO;
    }

    /**
     * 构建热点文章 VO
     */
    private HotArticleVO buildHotArticleVO(Article article) {
        HotArticleVO hotArticleVO = new HotArticleVO();
        hotArticleVO.setId(article.getId());
        hotArticleVO.setTitle(article.getTitle());
        hotArticleVO.setSummary(article.getSummary());
        hotArticleVO.setCoverImage(article.getCoverImage());
        
        // 获取实际浏览量（DB 基础值 + Redis 增量值）
        Long redisViewCount = viewCountCache.getViewCount(article.getId());
        int dbViewCount = article.getViewCount() != null ? article.getViewCount() : 0;
        int viewCount = dbViewCount + (redisViewCount != null ? redisViewCount.intValue() : 0);
        hotArticleVO.setViewCount(viewCount);
        
        // 获取实际点赞数（DB 基础值 + Redis 增量值）
        Long redisLikeCount = likeCache.getLikeCount(article.getId());
        int dbLikeCount = article.getLikeCount() != null ? article.getLikeCount() : 0;
        int likeCount = dbLikeCount + (redisLikeCount != null ? redisLikeCount.intValue() : 0);
        hotArticleVO.setLikeCount(likeCount);
        
        if (article.getCreateTime() != null) {
            hotArticleVO.setCreateTime(article.getCreateTime().toString());
        }
        
        return hotArticleVO;
    }

    /**
     * 构建热点文章 VO（从 ArticleVO）
     */
    private HotArticleVO buildHotArticleVO(ArticleVO articleVO) {
        HotArticleVO hotArticleVO = new HotArticleVO();
        hotArticleVO.setId(articleVO.getId());
        hotArticleVO.setTitle(articleVO.getTitle());
        hotArticleVO.setSummary(articleVO.getSummary());
        hotArticleVO.setCoverImage(articleVO.getCoverImage());
        
        // 获取实际浏览量（DB 基础值 + Redis 增量值）
        Long redisViewCount = viewCountCache.getViewCount(articleVO.getId());
        int dbViewCount = articleVO.getViewCount() != null ? articleVO.getViewCount() : 0;
        int viewCount = dbViewCount + (redisViewCount != null ? redisViewCount.intValue() : 0);
        hotArticleVO.setViewCount(viewCount);
        
        // 获取实际点赞数（DB 基础值 + Redis 增量值）
        Long redisLikeCount = likeCache.getLikeCount(articleVO.getId());
        int dbLikeCount = articleVO.getLikeCount() != null ? articleVO.getLikeCount() : 0;
        int likeCount = dbLikeCount + (redisLikeCount != null ? redisLikeCount.intValue() : 0);
        hotArticleVO.setLikeCount(likeCount);
        
        if (articleVO.getCreateTime() != null) {
            hotArticleVO.setCreateTime(articleVO.getCreateTime().toString());
        }
        
        return hotArticleVO;
    }
}
