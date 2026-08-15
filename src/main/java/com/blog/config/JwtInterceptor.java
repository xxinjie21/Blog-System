package com.blog.config;

import com.blog.annotation.RequiresPermission;
import com.blog.controller.Result;
import com.blog.service.PermissionService;
import com.blog.service.TokenService;
import com.blog.util.JwtUtil;
import com.blog.util.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录 + RBAC 权限拦截器
 *
 * 【校验链路】
 * 1. JWT 验签 → 证明"是服务器签的"
 * 2. Redis 查 access 令牌存在 → 证明"没被登出"
 * 3. Redis 查 user 会话标记 → 证明"账号没被禁用/密码没改"
 * 4. 【RBAC】查 @RequiresPermission 注解 → 证明"有权限做这个操作"
 *
 * 1-3 不通过 → 401（未登录/失效）
 * 4 不通过 → 403（无权限）
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // 1. 跨域预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // 2. 从请求头取令牌
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 3. 没有令牌 → 401 未登录
        if (token == null || token.isEmpty()) {
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        // 4. JWT 验签 + Redis 三层校验
        try {
            Claims claims = jwtUtil.parseAccessToken(token);
            Long userId = Long.valueOf(claims.getSubject());

            if (!tokenService.validateAccessToken(token)) {
                writeUnauthorized(response, "登录已失效，请重新登录");
                return false;
            }

            if (!tokenService.isUserActive(userId)) {
                writeUnauthorized(response, "账号状态异常，请重新登录");
                return false;
            }

            UserContext.setUserId(userId);

            // 5. RBAC 权限校验（@RequiresPermission 注解）
            if (handler instanceof HandlerMethod handlerMethod) {
                RequiresPermission annotation = handlerMethod.getMethodAnnotation(RequiresPermission.class);
                if (annotation != null) {
                    String requiredCode = annotation.value();
                    if (!permissionService.hasPermission(userId, requiredCode)) {
                        writeForbidden(response, "无权限执行此操作: " + requiredCode);
                        return false;
                    }
                }
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, message)));
    }

    private void writeForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(403, message)));
    }
}
