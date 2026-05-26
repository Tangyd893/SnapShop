package com.snapshop.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.snapshop.auth.dto.LoginDTO;
import com.snapshop.auth.dto.RefreshTokenDTO;
import com.snapshop.auth.dto.RegisterDTO;
import com.snapshop.auth.entity.User;
import com.snapshop.auth.mapper.UserMapper;
import com.snapshop.auth.service.AuthService;
import com.snapshop.auth.util.JwtUtil;
import com.snapshop.auth.util.PasswordUtil;
import com.snapshop.auth.vo.*;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String TOKEN_BLACKLIST_PREFIX = "auth:token:blacklist:";
    private static final String DEFAULT_NICKNAME_PREFIX = "用户";

    @Override
    public RegisterVO register(RegisterDTO dto) {
        // 校验用户名唯一性
        User existByUsername = userMapper.selectByUsername(dto.getUsername());
        if (existByUsername != null) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        // 校验手机号唯一性
        User existByPhone = userMapper.selectByPhone(dto.getPhone());
        if (existByPhone != null) {
            throw new BizException(ErrorCode.PHONE_EXISTS);
        }

        // 构建用户实体
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordUtil.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setNickname(DEFAULT_NICKNAME_PREFIX + dto.getUsername());
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        log.info("用户注册成功, userId={}, username={}", user.getId(), user.getUsername());

        return RegisterVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 支持用户名或手机号登录
        User user = userMapper.selectByUsername(dto.getAccount());
        if (user == null) {
            user = userMapper.selectByPhone(dto.getAccount());
        }
        if (user == null) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }

        // 校验密码
        if (!passwordUtil.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }

        // 生成令牌
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        log.info("用户登录成功, userId={}, username={}", user.getId(), user.getUsername());

        return LoginVO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenTtl())
                .user(UserInfo.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .build())
                .build();
    }

    @Override
    public void logout(String token) {
        String jti = jwtUtil.getJti(token);
        if (jti == null) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        long remainingTtlSeconds = jwtUtil.getRemainingTtlSeconds(token);
        if (remainingTtlSeconds <= 0) {
            return;
        }

        // 将 JTI 写入 Redis 黑名单
        String key = TOKEN_BLACKLIST_PREFIX + jti;
        stringRedisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(remainingTtlSeconds));

        log.info("令牌已加入黑名单, jti={}, ttl={}s", jti, remainingTtlSeconds);
    }

    @Override
    public RefreshTokenVO refreshToken(RefreshTokenDTO dto) {
        String refreshToken = dto.getRefreshToken();

        // 校验刷新令牌
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        // 校验是否在黑名单中
        String jti = jwtUtil.getJti(refreshToken);
        if (jti != null && Boolean.TRUE.equals(stringRedisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + jti))) {
            throw new BizException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);

        // 生成新的访问令牌
        String newAccessToken = jwtUtil.generateAccessToken(userId, username);

        log.info("刷新令牌成功, userId={}, username={}", userId, username);

        return RefreshTokenVO.builder()
                .accessToken(newAccessToken)
                .expiresIn(jwtUtil.getAccessTokenTtl())
                .build();
    }
}
