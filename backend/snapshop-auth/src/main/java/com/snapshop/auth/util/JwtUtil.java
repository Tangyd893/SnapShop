package com.snapshop.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类
 */
@Component
@RefreshScope
public class JwtUtil {

    @Value("${snapshop.jwt.secret:SnapShopJwtSecretKey2024MustBeAtLeast32Bytes!}")
    private String secret;

    @Value("${snapshop.jwt.access-token-ttl-seconds:7200}")
    private int accessTokenTtl;

    @Value("${snapshop.jwt.refresh-token-ttl-seconds:604800}")
    private int refreshTokenTtl;

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    /**
     * 获取 HMAC-SHA256 签名密钥
     */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(Long userId, String username) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenTtl * 1000L);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .id(jti)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Long userId, String username) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenTtl * 1000L);

        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .id(jti)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    /**
     * 获取访问令牌过期时间（秒）
     */
    public int getAccessTokenTtl() {
        return accessTokenTtl;
    }

    /**
     * 解析令牌
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * 校验令牌是否有效
     */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /**
     * 校验是否为刷新令牌
     */
    public boolean isRefreshToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return false;
        }
        return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE));
    }

    /**
     * 从令牌中获取用户编号
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从令牌中获取用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get(CLAIM_USERNAME, String.class);
    }

    /**
     * 从令牌中获取 JTI
     */
    public String getJti(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.getId();
    }

    /**
     * 获取令牌的剩余有效时间（秒）
     */
    public long getRemainingTtlSeconds(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return 0;
        }
        long diff = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(diff / 1000, 0);
    }
}
