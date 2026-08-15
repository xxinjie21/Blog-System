package com.blog.service;

import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.blog.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 认证服务接口 - 注册、登录、刷新、登出、修改密码
 *
 * 【企业级设计】
 * - 双令牌：login 返回 access + refresh；refresh 换新；logout 撤销；changePassword 踢下线所有会话
 * - 所有敏感操作写审计日志（blog_login_log）
 */
public interface AuthService {

    /**
     * 用户注册
     */
    Long register(RegisterDTO registerDTO, HttpServletRequest request);

    /**
     * 用户登录（防暴力破解 + 签发双令牌）
     */
    Map<String, Object> login(LoginDTO loginDTO, HttpServletRequest request);

    /**
     * 刷新令牌：换发新的访问令牌（并轮换刷新令牌）
     */
    Map<String, Object> refresh(String refreshToken, HttpServletRequest request);

    /**
     * 登出：撤销当前访问令牌 + 回收该用户全部刷新令牌
     */
    void logout(String accessToken, HttpServletRequest request);

    /**
     * 修改密码：校验旧密码 → 更新 → 踢下线该用户所有会话
     */
    void changePassword(ChangePasswordDTO dto, Long userId, HttpServletRequest request);

    /**
     * 获取当前用户信息（脱敏返回）
     */
    UserVO getCurrentUser(Long userId);
}
