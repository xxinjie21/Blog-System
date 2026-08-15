package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.dto.ChangePasswordDTO;
import com.blog.dto.LoginDTO;
import com.blog.dto.RegisterDTO;
import com.blog.entity.LoginLog;
import com.blog.entity.User;
import com.blog.exception.BlogException;
import com.blog.mapper.LoginLogMapper;
import com.blog.mapper.UserMapper;
import com.blog.mapper.UserRoleMapper;
import com.blog.service.AuthService;
import com.blog.service.TokenService;
import com.blog.util.JwtUtil;
import com.blog.vo.UserVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务实现类
 *
 * 【企业级考点】
 * 1. 密码必须 BCrypt 加密存储，数据库绝不存明文（盐随机、不可逆、防彩虹表）
 * 2. 登录防暴力破解：Redis 计数，连续失败 N 次锁定 M 分钟
 * 3. 双令牌：短时效 access + 长时效 refresh，Refresh 时轮换旧刷新令牌
 * 4. 登出/禁用/改密即时生效：Redis 会话撤销（TokenService）
 * 5. 审计日志：注册/登录成功/失败/登出/刷新/改密全部留痕
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final LoginLogMapper loginLogMapper;
    private final UserRoleMapper userRoleMapper;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 连续失败多少次锁定 */
    @Value("${jwt.login-lock.max-fail-attempts:5}")
    private int maxFailAttempts;

    /** 锁定多少分钟 */
    @Value("${jwt.login-lock.lock-minutes:10}")
    private long lockMinutes;

    /** 访问令牌有效期（分钟） */
    @Value("${jwt.access-expire-minutes:30}")
    private long accessExpireMinutes;

    /** 刷新令牌有效期（天） */
    @Value("${jwt.refresh-expire-days:7}")
    private long refreshExpireDays;

    // ==================== 注册 ====================

    @Override
    public Long register(RegisterDTO registerDTO, HttpServletRequest request) {
        // 1. 用户名查重（uk_username 唯一索引兜底）
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

        // 3. 组装用户并 BCrypt 加密密码
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setNickname(StringUtils.hasText(registerDTO.getNickname())
                ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setEmail(registerDTO.getEmail());
        user.setStatus(1);

        userMapper.insert(user);
        userRoleMapper.assignRole(user.getId(), "user");   // 新用户自动分配 user 角色
        saveAuditLog(user.getId(), registerDTO.getUsername(), "REGISTER", request, "注册成功");
        return user.getId();
    }

    // ==================== 登录（防爆破 + 双令牌） ====================

    @Override
    public Map<String, Object> login(LoginDTO loginDTO, HttpServletRequest request) {
        String username = loginDTO.getUsername();

        // 1. 检查是否被锁定（暴力破解防护）
        checkLocked(username);

        // 2. 按用户名查用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));
        if (user == null) {
            recordLoginFailure(username, request, "用户不存在");
            throw new BlogException(400, "用户名或密码错误");
        }

        // 3. 校验密码（BCrypt matches 比对）
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            recordLoginFailure(username, request, "密码错误");
            throw new BlogException(400, "用户名或密码错误");
        }

        // 4. 校验账号状态
        if (user.getStatus() == 0) {
            saveAuditLog(user.getId(), username, "LOGIN_FAIL", request, "账号已被禁用");
            throw new BlogException(403, "账号已被禁用");
        }

        // 5. 登录成功：清失败计数、标记会话、签发双令牌
        clearLoginFailure(username);
        tokenService.markUserActive(user.getId(), refreshExpireDays);

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
        tokenService.saveAccessToken(accessToken, user.getId(), accessExpireMinutes);
        tokenService.saveRefreshToken(refreshToken, user.getId(), refreshExpireDays);

        saveAuditLog(user.getId(), username, "LOGIN_SUCCESS", request, "登录成功");

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", accessToken);
        data.put("refreshToken", refreshToken);
        data.put("expiresIn", accessExpireMinutes * 60);   // 秒
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        return data;
    }

    // ==================== 刷新令牌 ====================

    @Override
    public Map<String, Object> refresh(String refreshToken, HttpServletRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.parseRefreshToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            saveAuditLog(null, "unknown", "REFRESH", request, "刷新令牌签名无效或过期");
            throw new BlogException(401, "刷新令牌无效，请重新登录");
        }

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);

        // Redis 校验：刷新令牌未被回收 + 用户会话标记存在
        if (!tokenService.validateRefreshToken(refreshToken, userId)) {
            saveAuditLog(userId, username, "REFRESH", request, "刷新令牌已回收");
            throw new BlogException(401, "刷新令牌已失效，请重新登录");
        }
        if (!tokenService.isUserActive(userId)) {
            saveAuditLog(userId, username, "REFRESH", request, "账号会话已失效");
            throw new BlogException(401, "账号会话已失效，请重新登录");
        }

        // 签发新令牌并轮换刷新令牌（旧刷新令牌作废，防重放）
        String newAccessToken = jwtUtil.generateAccessToken(userId, username);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username);
        tokenService.saveAccessToken(newAccessToken, userId, accessExpireMinutes);
        tokenService.rotateRefreshToken(refreshToken, newRefreshToken, userId, refreshExpireDays);

        saveAuditLog(userId, username, "REFRESH", request, "令牌刷新成功");

        Map<String, Object> data = new HashMap<>();
        data.put("accessToken", newAccessToken);
        data.put("refreshToken", newRefreshToken);
        data.put("expiresIn", accessExpireMinutes * 60);
        return data;
    }

    // ==================== 登出 ====================

    @Override
    public void logout(String accessToken, HttpServletRequest request) {
        Claims claims;
        try {
            claims = jwtUtil.parseAccessToken(accessToken);
        } catch (JwtException | IllegalArgumentException e) {
            return;   // 令牌已失效，登出幂等，直接返回
        }

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);

        // 撤销当前访问令牌 + 回收该用户全部刷新令牌
        tokenService.removeAccessToken(accessToken);
        tokenService.revokeUserRefreshTokens(userId);

        saveAuditLog(userId, username, "LOGOUT", request, "用户登出");
    }

    // ==================== 修改密码 ====================

    @Override
    public void changePassword(ChangePasswordDTO dto, Long userId, HttpServletRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BlogException(404, "用户不存在");
        }

        // 1. 校验旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            saveAuditLog(userId, user.getUsername(), "CHANGE_PASSWORD", request, "旧密码错误");
            throw new BlogException(400, "旧密码错误");
        }

        // 2. 新旧密码相同直接拒绝
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BlogException(400, "新密码不能与旧密码相同");
        }

        // 3. 确认密码一致
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BlogException(400, "两次输入的密码不一致");
        }

        // 4. 更新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);

        // 5. 踢下线所有会话：已签发的 access/refresh 全部失效，必须重新登录
        tokenService.killUserSessions(userId);

        saveAuditLog(userId, user.getUsername(), "CHANGE_PASSWORD", request, "修改密码成功，全部会话已下线");
    }

    // ==================== 获取用户信息（脱敏） ====================

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BlogException(404, "用户不存在");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        return vo;   // Jackson 序列化时 @Sensitive 自动脱敏
    }

    // ==================== 私有方法 ====================

    /**
     * 检查是否被锁定
     */
    private void checkLocked(String username) {
        if (tokenService.isLocked(username)) {
            throw new BlogException(429, "尝试次数过多，账号已锁定，请 " + lockMinutes + " 分钟后再试");
        }
    }

    /**
     * 记录一次登录失败，达到阈值则锁定
     */
    private void recordLoginFailure(String username, HttpServletRequest request, String detail) {
        Long failCount = tokenService.incrementFailCount(username, lockMinutes);
        saveAuditLog(null, username, "LOGIN_FAIL", request, detail + "（第 " + failCount + " 次失败）");
        if (failCount >= maxFailAttempts) {
            tokenService.lockUser(username, lockMinutes);
        }
    }

    /**
     * 登录成功清空失败计数
     */
    private void clearLoginFailure(String username) {
        tokenService.clearFailCount(username);
    }

    /**
     * 写审计日志（表 + 日志双通道）
     */
    private void saveAuditLog(Long userId, String username, String operation,
                              HttpServletRequest request, String detail) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setOperation(operation);
        loginLog.setIpAddress(getClientIp(request));
        loginLog.setDetail(detail);
        loginLogMapper.insert(loginLog);

        log.info("[审计] {} | 用户:{} | IP:{} | 详情:{}", operation, username,
                getClientIp(request), detail);
    }

    /**
     * 获取客户端真实 IP（兼容反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
