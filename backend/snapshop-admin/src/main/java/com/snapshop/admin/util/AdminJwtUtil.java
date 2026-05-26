package com.snapshop.admin.util;

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
 * 管理后台 JWT 工具类（与 C 端隔离的密钥和 issuser）
 */
@Component
@RefreshScope
public class AdminJwtUtil {

    @Value("${snapshop.admin.jwt.secret:AdminJwtSecretKey2024DifferentFromcEnd!}")
    private String secret;

    @Value("${snapshop.admin.jwt.access-token-ttl-seconds:7200}")
    private int accessTokenTtl;

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "type";
    private static final String TOKEN_TYPE_ACCESS = "admin_access";
    private static final String ISSUER = "snapshop-admin";

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成管理后台 JWT
     */
    public String generateAccessToken(Long adminId, String username, String role) {
        String jti = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenTtl * 1000L);

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(adminId.toString())
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .id(jti)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    /**
     * 获取令牌过期时间（秒）
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
                    .requireIssuer(ISSUER)
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
     * 从令牌中获取管理员编号
     */
    public Long getAdminId(String token) {
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
     * 从令牌中获取角色
     */
    public String getRole(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return claims.get(CLAIM_ROLE, String.class);
    }
}
