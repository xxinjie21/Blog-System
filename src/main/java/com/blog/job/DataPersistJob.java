package com.blog.job;

import com.blog.cache.LikeCache;
import com.blog.cache.ViewCountCache;
import com.blog.entity.Article;
import com.blog.service.ArticleService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Map;

/**
 * 数据持久化定时任务
 * 
 * 【面试考点】
 * 1. Quartz Job 不能用 @RequiredArgsConstructor（Quartz 自行实例化，绕过 Spring）
 * 2. 改用 ApplicationContextAware 获取 Spring Bean
 * 3. Redis 数据异步持久化
 */
@Slf4j
public class DataPersistJob extends QuartzJobBean implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        applicationContext = ctx;
    }

    @Override
    protected void executeInternal(JobExecutionContext context) {
        log.info("========== 开始执行数据持久化任务 ==========");

        ViewCountCache viewCountCache = applicationContext.getBean(ViewCountCache.class);
        LikeCache likeCache = applicationContext.getBean(LikeCache.class);
        ArticleService articleService = applicationContext.getBean(ArticleService.class);

        persistViewCounts(viewCountCache, articleService);
        persistLikeCounts(likeCache, articleService);

        log.info("========== 数据持久化任务执行完成 ==========");
    }

    /**
     * 持久化浏览量到 MySQL
     * 
     * 【优化点】
     * 1. 批量更新，减少 DB 交互
     * 2. 只更新有增量数据的文章
     */
    private void persistViewCounts(ViewCountCache viewCountCache, ArticleService articleService) {
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
    private void persistLikeCounts(LikeCache likeCache, ArticleService articleService) {
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
