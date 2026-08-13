package com.blog.controller;

import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.blog.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器 - 注册、登录
 *
 * 【面试考点】
 * 1. 登录接口返回 JWT 令牌，前端存起来每次请求带上
 * 2. 注册接口用 @Validated 做参数前置校验
 */
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
    @PostMapping("/register")
    public Result<Long> register(@RequestBody @Validated RegisterDTO registerDTO) {
        return Result.success(authService.register(registerDTO));
    }

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 令牌 + 用户信息
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Validated LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }
}
