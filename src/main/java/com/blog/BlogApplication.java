package com.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SpringBoot 启动类
 * 
 * 【面试考点】
 * 1. @SpringBootApplication 组合注解
 * 2. @MapperScan 扫描 Mapper 接口
 * 3. @EnableAspectJAutoProxy 开启 AOP
 */
@SpringBootApplication
@MapperScan("com.blog.mapper")
@EnableAspectJAutoProxy
@EnableScheduling
public class BlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
    }
}
