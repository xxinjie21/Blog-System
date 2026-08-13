package com.blog.config;

import com.blog.util.JwtUtil;
import com.blog.util.UserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器
 *
 * 【面试考点】
 * 1. 认证流程：取请求头 Authorization → 验签 → 通过则把 userId 放进 ThreadLocal 放行
 * 2. 401 未登录 / 403 已登录但无权限
 * 3. OPTIONS 预检请求直接放行（跨域场景）
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // 1. 跨域预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // 2. 从请求头取令牌：Authorization: Bearer xxx
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 3. 没有令牌 = 未登录
        if (token == null || token.isEmpty()) {
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        // 4. 验签：签名非法或过期会抛 JwtException
        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.valueOf(claims.getSubject());
            UserContext.setUserId(userId);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }
    }

    /**
     * 请求结束清理 ThreadLocal，防止线程池复用导致数据串号
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    /**
     * 输出 401 未登录响应
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"timestamp\":"
                + System.currentTimeMillis() + "}");
    }
}
