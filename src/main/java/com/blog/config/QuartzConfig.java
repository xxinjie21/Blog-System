package com.blog.config;

import com.blog.job.DataPersistJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 定时任务配置类
 * 
 * 【面试考点】
 * 1. JobDetail 配置
 * 2. Trigger 触发器配置
 * 3. Cron 表达式
 */
@Configuration
public class QuartzConfig {

    /**
     * 配置数据持久化任务详情
     * 
     * @return JobDetail
     */
    @Bean
    public JobDetail dataPersistJobDetail() {
        return JobBuilder.newJob(DataPersistJob.class)
                .withIdentity("dataPersistJob", "blogJobs")
                .withDescription("数据持久化任务：将 Redis 中的浏览量和点赞数同步到 MySQL")
                .storeDurably()
                .build();
    }

    /**
     * 配置数据持久化任务触发器
     * 
     * 【优化点】
     * 1. 每 5 分钟执行一次
     * 2. Cron 表达式：0 0/5 * * * ?
     * 
     * @return Trigger
     */
    @Bean
    public Trigger dataPersistTrigger() {
        // Cron 表达式说明：
        // 秒  分   时   日   月   周
        // 0   0/5  *    *    *    ?  表示每 5 分钟执行一次
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0/5 * * * ?");
        
        return TriggerBuilder.newTrigger()
                .forJob(dataPersistJobDetail())
                .withIdentity("dataPersistTrigger", "blogTriggers")
                .withSchedule(scheduleBuilder)
                .withDescription("每 5 分钟执行一次数据持久化")
                .build();
    }
}
