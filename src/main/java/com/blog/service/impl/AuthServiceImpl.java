package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.blog.entity.User;
import com.blog.exception.BlogException;
import com.blog.mapper.UserMapper;
import com.blog.service.AuthService;
import com.blog.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现类
 *
 * 【面试考点】
 * 1. 密码必须 BCrypt 加密存储，数据库绝不存明文
 * 2. BCrypt 每次加密盐值随机，同一密码两次结果不同，防彩虹表
 * 3. 登录时用 matches 比对，而不是解密（BCrypt 不可逆）
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户注册
     */
    @Override
    public Long register(RegisterDTO registerDTO) {
        // 1. 检查用户名是否已存在（uk_username 唯一索引兜底）
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, registerDTO.getUsername()));
        if (count > 0) {
            throw new BlogException(400, "用户名已存在");
        }

        // 2. 邮箱查重（uk_email 唯一索引兜底）
        if (StringUtils.hasText(registerDTO.getEmail())) {
            count = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getEmail, registerDTO.getEmail()));
            if (count > 0) {
                throw new BlogException(400, "邮箱已被注册");
            }
        }

        // 3. 组装用户并加密密码
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(StringUtils.hasText(registerDTO.getNickname())
                ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setStatus(1);

        userMapper.insert(user);
        return user.getId();
    }

    /**
     * 用户登录
     */
    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        // 1. 按用户名查用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, loginDTO.getUsername()));
        if (user == null) {
            throw new BlogException(400, "用户名或密码错误");
        }

        // 2. 校验密码（BCrypt matches 比对）
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BlogException(400, "用户名或密码错误");
        }

        // 3. 校验账号状态
        if (user.getStatus() == 0) {
            throw new BlogException(403, "账号已被禁用");
        }

        // 4. 签发 JWT 令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 5. 返回令牌和脱敏后的用户信息
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        return data;
    }
}
