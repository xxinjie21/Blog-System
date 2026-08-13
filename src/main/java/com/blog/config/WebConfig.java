package com.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Web 配置类 - 跨域处理 + 拦截器注册
 * 
 * 【面试考点】
 * 1. 跨域问题解决方案
 * 2. CorsFilter 配置
 * 3. 登录拦截器注册：哪些路径需要登录、哪些放行
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    /**
     * 注册登录拦截器
     * 
     * 【学习点】
     * addPathPatterns 哪些路径要拦，excludePathPatterns 哪些放行
     * 放行：注册、登录、文章查询等公开接口
     * 拦截：写操作（发布/修改/删除文章）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/register",
                        "/auth/login",
                        "/articles/page",
                        "/articles/hot",
                        "/articles/{id}",
                        "/articles/{id}/view",
                        "/category/**",
                        "/tag/**",
                        "/comment/**",
                        "/error"
                );
    }

    /**
     * 配置跨域过滤器
     * 
     * 【优化点】
     * 1. 支持所有来源（生产环境应限制具体域名）
     * 2. 支持常用 HTTP 方法
     * 3. 允许携带 Cookie
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源（生产环境应指定具体域名）
        config.addAllowedOriginPattern("*");
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
