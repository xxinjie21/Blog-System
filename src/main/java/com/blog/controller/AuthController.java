package com.blog.controller;

import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.LoginDTO;
import com.blog.dto.RefreshDTO;
import com.blog.dto.RegisterDTO;
import com.blog.service.AuthService;
import com.blog.util.UserContext;
import com.blog.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器 - 注册、登录、刷新、登出、修改密码
 *
 * 【企业级设计】
 * 1. 登录返回双令牌：accessToken（短时效）+ refreshToken（长时效）
 * 2. refresh：用刷新令牌换新访问令牌，无需重新登录
 * 3. logout / password：必须登录后调用（拦截器校验）
 */
@Tag(name = "认证接口", description = "注册、登录、刷新、登出、修改密码、查看个人信息")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     *
     * @param registerDTO 注册信息
     * @return 新用户 ID
     */
    @Operation(summary = "用户注册", description = "注册新用户，自动分配 user 角色")
    @PostMapping("/register")
    public Result<Long> register(@RequestBody @Validated RegisterDTO registerDTO,
                                 HttpServletRequest request) {
        return Result.success(authService.register(registerDTO, request));
    }

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 访问令牌 + 刷新令牌 + 用户信息
     */
    @Operation(summary = "用户登录", description = "返回 accessToken（30分钟）+ refreshToken（7天）")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Validated LoginDTO loginDTO,
                                             HttpServletRequest request) {
        return Result.success(authService.login(loginDTO, request));
    }

    /**
     * 刷新令牌（登录过期后换取新访问令牌）
     *
     * @param refreshDTO 刷新令牌
     * @return 新的访问令牌 + 新的刷新令牌
     */
    @Operation(summary = "刷新令牌", description = "用 refreshToken 换新的 access + refresh")
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody @Validated RefreshDTO refreshDTO,
                                               HttpServletRequest request) {
        return Result.success(authService.refresh(refreshDTO.getRefreshToken(), request));
    }

    /**
     * 登出（撤销令牌，立即失效）
     */
    @Operation(summary = "登出", description = "撤销当前令牌 + 回收全部刷新令牌，立即失效")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractBearerToken(request);
        authService.logout(token, request);
        return Result.success();
    }

    /**
     * 修改密码（改后所有会话立即下线）
     *
     * @param changePasswordDTO 旧密码 + 新密码
     */
    @Operation(summary = "修改密码", description = "校验旧密码并更新，改后所有会话踢下线")
    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody @Validated ChangePasswordDTO changePasswordDTO,
                                       HttpServletRequest request) {
        authService.changePassword(changePasswordDTO, UserContext.getUserId(), request);
        return Result.success();
    }

    /**
     * 获取当前登录用户信息（脱敏返回）
     *
     * 展示 @Sensitive 注解效果：email → z***@gmail.com, phone → 138****8000
     */
    @Operation(summary = "获取当前用户信息", description = "返回脱敏后的用户信息（email/phone 脱敏）")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser() {
        return Result.success(authService.getCurrentUser(UserContext.getUserId()));
    }

    /**
     * 从请求头提取 Bearer 令牌
     */
    private String extractBearerToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return "";
    }
}
