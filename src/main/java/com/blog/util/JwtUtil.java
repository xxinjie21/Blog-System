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
 * JWT 工具类 - 令牌签发与校验
 *
 * 【面试考点】
 * 1. JWT 三段结构：Header.Payload.Signature
 * 2. Payload 存 userId，签名用密钥防篡改
 * 3. 无状态认证：服务器不存会话，验签名即可
 */
@Component
public class JwtUtil {

    /**
     * 签名密钥（HS256 要求至少 32 字节）
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * 令牌有效期（小时）
     */
    @Value("${jwt.expire-hours}")
    private Long expireHours;

    private SecretKey key;

    /**
     * 初始化签名密钥
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT 令牌
     *
     * @param userId 用户 ID
     * @param username 用户名
     * @return 签发的令牌字符串
     */
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireHours * 3600 * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))          // 标准字段：主体 = userId
                .claim("username", username)              // 自定义字段：用户名
                .issuedAt(now)                            // 签发时间
                .expiration(expiry)                       // 过期时间
                .signWith(key)                            // 用服务器密钥签名
                .compact();
    }

    /**
     * 解析令牌并校验签名
     *
     * @param token 令牌
     * @return 载荷（含 userId、username），签名非法或过期会抛异常
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)                          // 用同一个密钥验签
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
