package com.blog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * Web 配置类 - 跨域处理 + 拦截器注册
 *
 * 【企业级考点】
 * 1. 登录拦截器注册：addPathPatterns 拦什么，excludePathPatterns 放什么
 * 2. CORS 必须收口为具体域名列表（生产禁止 *），可配置
 * 3. 放行规则与业务对齐：公开查询放行，写操作必须登录
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /**
     * 允许的跨域来源（配置文件注入，生产环境必须收敛为具体域名）
     */
    @Value("${blog.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * 注册登录拦截器
     *
     * 放行：注册、登录、刷新、公开查询接口
     * 拦截：写操作（发布/修改/删除文章、评论写操作、登出、改密）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 认证公开接口
                        "/auth/register",
                        "/auth/login",
                        "/auth/refresh",
                        // 公开查询接口
                        "/articles/page",
                        "/articles/hot",
                        "/articles/{id}",
                        "/articles/{id}/view",
                        "/category/**",
                        "/tag/**",
                        "/comments/article/**",
                        // Swagger / OpenAPI 文档
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/error"
                );
    }

    /**
     * 配置跨域过滤器（生产环境必须配置具体域名）
     *
     * 【企业级考点】
     * 1. allowedOrigins 从配置读，生产用环境变量 CORS_ALLOWED_ORIGINS 覆盖
     * 2. 禁止 * + credentials 组合（浏览器安全策略不允许）
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的跨域来源：从配置读取（默认本地前端开发地址）
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 允许所有请求方法
        config.addAllowedMethod("*");
        // 允许携带 Cookie
        config.setAllowCredentials(true);
        // 预检请求缓存时间
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
