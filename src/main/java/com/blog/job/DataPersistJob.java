package com.blog.job;

import com.blog.cache.LikeCache;
import com.blog.cache.ViewCountCache;
import com.blog.entity.Article;
import com.blog.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Map;

/**
 * 数据持久化定时任务
 * 
 * 【面试考点】
 * 1. Quartz 定时任务配置
 * 2. Redis 数据异步持久化
 * 3. 批量更新优化
 * 
 * 【优化思路】
 * - 每 5 分钟执行一次
 * - 批量更新浏览量和点赞数
 * - 减少 DB 交互次数
 */
@Slf4j
@RequiredArgsConstructor
public class DataPersistJob extends QuartzJobBean {

    private final ViewCountCache viewCountCache;
    private final LikeCache likeCache;
    private final ArticleService articleService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("========== 开始执行数据持久化任务 ==========");

        // 1. 持久化浏览量
        persistViewCounts();

        // 2. 持久化点赞数
        persistLikeCounts();

        log.info("========== 数据持久化任务执行完成 ==========");
    }

    /**
     * 持久化浏览量到 MySQL
     * 
     * 【优化点】
     * 1. 批量更新，减少 DB 交互
     * 2. 只更新有增量数据的文章
     */
    private void persistViewCounts() {
        try {
            Map<Long, Long> viewCounts = viewCountCache.getAllViewCounts();
            
            if (viewCounts != null && !viewCounts.isEmpty()) {
                log.info("本次需要持久化浏览量的文章数：{}", viewCounts.size());
                
                // 批量更新浏览量
                for (Map.Entry<Long, Long> entry : viewCounts.entrySet()) {
                    Long articleId = entry.getKey();
                    Long redisCount = entry.getValue();
                    
                    if (redisCount != null && redisCount > 0) {
                        Article article = articleService.getById(articleId);
                        if (article != null) {
                            // 更新 DB 基础值
                            article.setViewCount(article.getViewCount() + redisCount.intValue());
                            articleService.updateById(article);
                            
                            // 重置 Redis 计数为 0（基于 DB 新基础值）
                            viewCountCache.setViewCount(articleId, 0L);
                            
                            log.debug("文章 {} 浏览量持久化：DB={}, Redis={}", 
                                    articleId, article.getViewCount(), redisCount);
                        }
                    }
                }
                
                log.info("浏览量持久化完成");
            }
        } catch (Exception e) {
            log.error("浏览量持久化失败", e);
        }
    }

    /**
     * 持久化点赞数到 MySQL
     * 
     * 【优化点】
     * 1. 批量更新，减少 DB 交互
     * 2. 只更新有增量数据的文章
     */
    private void persistLikeCounts() {
        try {
            Map<Long, Long> likeCounts = likeCache.getAllLikeCounts();
            
            if (likeCounts != null && !likeCounts.isEmpty()) {
                log.info("本次需要持久化点赞数的文章数：{}", likeCounts.size());
                
                // 批量更新点赞数
                for (Map.Entry<Long, Long> entry : likeCounts.entrySet()) {
                    Long articleId = entry.getKey();
                    Long redisCount = entry.getValue();
                    
                    if (redisCount != null && redisCount > 0) {
                        Article article = articleService.getById(articleId);
                        if (article != null) {
                            // 更新 DB 基础值
                            article.setLikeCount(article.getLikeCount() + redisCount.intValue());
                            articleService.updateById(article);
                            
                            // 重置 Redis 计数为 0（基于 DB 新基础值）
                            likeCache.setLikeCount(articleId, 0L);
                            
                            log.debug("文章 {} 点赞数持久化：DB={}, Redis={}", 
                                    articleId, article.getLikeCount(), redisCount);
                        }
                    }
                }
                
                log.info("点赞数持久化完成");
            }
        } catch (Exception e) {
            log.error("点赞数持久化失败", e);
        }
    }
}
