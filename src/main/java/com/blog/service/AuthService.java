package com.blog.service;

import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;

import java.util.Map;

/**
 * 认证服务接口 - 注册、登录
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param registerDTO 注册信息
     * @return 新用户 ID
     */
    Long register(RegisterDTO registerDTO);

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return token（令牌）和用户信息
     */
    Map<String, Object> login(LoginDTO loginDTO);
}
