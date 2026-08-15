package com.blog.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * XSS 防护过滤器
 *
 * 【企业级考点】
 * 1. URL 参数：重写 getParameter() 做清理（表单提交）
 * 2. JSON body：重写 getInputStream() 做清理（REST API）
 * 3. 两个入口都覆盖，对 Controller 透明
 *
 * 【防不住的情况】
 * - 富文本编辑器（文章正文需要 HTML）→ 需要 Jsoup 白名单过滤器
 * - 输出编码：前端渲染时也应对 < > 做 HTML 转义
 * - Content-Security-Policy 响应头（前端防线）
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter implements Filter {

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "<(script|iframe|object|embed|form|input|button|textarea|select)[\\s>]"
            + "|<(/?)(script|iframe|object|embed)[\\s>]"
            + "|on(\\w+)=['\"].*?['\"]"
            + "|javascript:"
            + "|<img[^>]+\\bonerror"
            + "|<svg[^>]+\\bonload",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String contentType = httpRequest.getContentType();

        if (contentType != null && contentType.contains("application/json")) {
            // JSON 请求：清理 body
            chain.doFilter(new XssJsonBodyWrapper(httpRequest), response);
        } else {
            // 表单/URL 参数请求：清理参数
            chain.doFilter(new XssParamWrapper(httpRequest), response);
        }
    }

    static String clean(String value) {
        if (value == null) return null;
        String cleaned = XSS_PATTERN.matcher(value).replaceAll("");
        if (!cleaned.equals(value)) {
            log.warn("[XSS] 拦截恶意输入: 原始={} | 清理后={}", value, cleaned);
        }
        return cleaned;
    }

    /**
     * 表单/URL 参数清理：重写 getParameter / getHeader
     */
    static class XssParamWrapper extends HttpServletRequestWrapper {
        public XssParamWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return clean(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) return null;
            String[] cleaned = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleaned[i] = clean(values[i]);
            }
            return cleaned;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return clean(value);
        }
    }

    /**
     * JSON body 清理：读取整个 body → 正则替换 → 替换为新的 InputStream
     * 对 @RequestBody 透明，Controller 不需要任何改动
     */
    static class XssJsonBodyWrapper extends HttpServletRequestWrapper {

        private byte[] cleanedBody;

        public XssJsonBodyWrapper(HttpServletRequest request) throws IOException {
            super(request);
            // 读取原始 body 并清理
            byte[] original = request.getInputStream().readAllBytes();
            String body = new String(original, StandardCharsets.UTF_8);
            String cleaned = clean(body);
            this.cleanedBody = cleaned.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ServletInputStream() {
                private final ByteArrayInputStream bis = new ByteArrayInputStream(cleanedBody);

                @Override
                public int read() { return bis.read(); }
                @Override
                public int available() { return bis.available(); }
                @Override
                public boolean isFinished() { return bis.available() == 0; }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setReadListener(ReadListener listener) {}
            };
        }

        @Override
        public int getContentLength() {
            return cleanedBody.length;
        }

        @Override
        public long getContentLengthLong() {
            return cleanedBody.length;
        }
    }
}
