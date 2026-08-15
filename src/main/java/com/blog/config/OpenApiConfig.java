package com.blog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置
 *
 * 【企业级考点】
 * 1. 基于 OpenAPI 3.0 规范自动生成 Swagger UI 文档
 * 2. 配置 JWT Bearer Token 认证方案 → Swagger UI 右上角可填 token
 * 3. 分组配置：按模块分组，方便查找接口
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI blogSystemOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("博客系统 API 文档")
                        .description("""
                                基于 Spring Boot 3.3 + Redis 缓存 + MyBatis-Plus 的博客系统。
                                功能模块：JWT 认证（双令牌）、RBAC 权限、文章管理、评论、分类标签。
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Blog System")
                                .email("admin@blog.com")))
                // JWT Bearer Token 认证方案
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 登录后获取 token，填入此框即可访问所有受保护接口")));
    }
}
