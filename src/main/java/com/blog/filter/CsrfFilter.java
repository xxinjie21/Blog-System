package com.blog.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * CSRF 防护过滤器
 *
 * 【企业级考点】
 * 1. JWT 放 Authorization Header 里 → 浏览器不会自动携带 → 天然防 CSRF
 * 2. 但作为"纵深防御"，写操作（POST/PUT/DELETE）额外检查自定义头 X-Request-Source
 * 3. 简单 HTML 表单无法设置自定义 Header → 伪造请求过不了这一关
 *
 * 防护链路：
 * 浏览器跨站表单提交 → 没有 X-Request-Source 头 → 403 拒绝
 * 正常前端请求       → 携带 X-Request-Source: web → 放行
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CsrfFilter implements Filter {

    private static final String HEADER_NAME = "X-Request-Source";
    private static final String HEADER_VALUE = "web";

    /** 允许不带 CSRF 头的白名单方法 */
    private static final String[] SAFE_METHODS = {"GET", "HEAD", "OPTIONS"};

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();

        // Swagger/OpenAPI 路径放行
        if (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/swagger-resources")) {
            chain.doFilter(request, response);
            return;
        }

        // 安全方法（GET/HEAD/OPTIONS）直接放行
        if (isSafeMethod(method)) {
            chain.doFilter(request, response);
            return;
        }

        // 写操作（POST/PUT/DELETE）检查自定义头
        String source = httpRequest.getHeader(HEADER_NAME);
        if (!HEADER_VALUE.equals(source)) {
            log.warn("[CSRF] 写操作缺少 {} 头: method={}, uri={}, ip={}",
                    HEADER_NAME, method, httpRequest.getRequestURI(),
                    httpRequest.getRemoteAddr());
            httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"code\":403,\"message\":\"CSRF 校验失败：缺少 " + HEADER_NAME + " 请求头\",\"timestamp\":" + System.currentTimeMillis() + "}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isSafeMethod(String method) {
        for (String safe : SAFE_METHODS) {
            if (safe.equalsIgnoreCase(method)) return true;
        }
        return false;
    }
}
