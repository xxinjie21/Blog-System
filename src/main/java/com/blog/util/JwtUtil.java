package com.blog.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 - 访问令牌/刷新令牌签发与校验
 *
 * 【企业级考点】
 * 1. 双令牌机制：短时效 access（30分钟）+ 长时效 refresh（7天），降低令牌泄露风险窗口
 * 2. 双密钥分离：access/refresh 用不同密钥，刷新令牌泄露也伪造不出访问令牌
 * 3. 无状态认证：JWT 自身携带身份信息 + 签名防篡改；会话撤销交给 TokenService（Redis）
 */
@Component
public class JwtUtil {

    /**
     * 访问令牌密钥（HS256 要求至少 32 字节，生产用环境变量注入）
     */
    @Value("${jwt.access-secret}")
    private String accessSecret;

    /**
     * 刷新令牌密钥（与访问令牌密钥分离）
     */
    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    /**
     * 访问令牌有效期（分钟）
     */
    @Value("${jwt.access-expire-minutes}")
    private Long accessExpireMinutes;

    /**
     * 刷新令牌有效期（天）
     */
    @Value("${jwt.refresh-expire-days}")
    private Long refreshExpireDays;

    private SecretKey accessKey;
    private SecretKey refreshKey;

    /**
     * 初始化两个签名密钥
     */
    @PostConstruct
    public void init() {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌（短时效）
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return 访问令牌
     */
    public String generateAccessToken(Long userId, String username) {
        return buildToken(accessKey, accessExpireMinutes * 60 * 1000, userId, username, "access");
    }

    /**
     * 生成刷新令牌（长时效）
     *
     * @param userId   用户 ID
     * @param username 用户名
     * @return 刷新令牌
     */
    public String generateRefreshToken(Long userId, String username) {
        return buildToken(refreshKey, refreshExpireDays * 24 * 3600 * 1000, userId, username, "refresh");
    }

    /**
     * 校验访问令牌
     *
     * @param token 访问令牌
     * @return 载荷，签名非法或过期抛 JwtException
     */
    public Claims parseAccessToken(String token) {
        return parseToken(accessKey, token);
    }

    /**
     * 校验刷新令牌
     *
     * @param token 刷新令牌
     * @return 载荷，签名非法或过期抛 JwtException
     */
    public Claims parseRefreshToken(String token) {
        return parseToken(refreshKey, token);
    }

    /**
     * 构建令牌
     */
    private String buildToken(SecretKey key, long ttlMillis, Long userId, String username, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))   // 标准字段：主体 = userId
                .claim("username", username)       // 自定义字段：用户名
                .claim("type", type)               // 自定义字段：access / refresh
                .issuedAt(now)                     // 签发时间
                .expiration(expiry)                // 过期时间
                .signWith(key)                     // 用对应密钥签名
                .compact();
    }

    /**
     * 解析并校验令牌签名
     */
    private Claims parseToken(SecretKey key, String token) {
        return Jwts.parser()
                .verifyWith(key)                   // 用对应密钥验签
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
