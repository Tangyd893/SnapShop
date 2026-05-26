package com.snapshop.auth.controller;

import com.snapshop.auth.dto.LoginDTO;
import com.snapshop.auth.dto.RefreshTokenDTO;
import com.snapshop.auth.dto.RegisterDTO;
import com.snapshop.auth.service.AuthService;
import com.snapshop.auth.vo.LoginVO;
import com.snapshop.auth.vo.RefreshTokenVO;
import com.snapshop.auth.vo.RegisterVO;
import com.snapshop.common.base.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public R<RegisterVO> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        RegisterVO vo = authService.register(dto);
        return R.ok(vo).requestId(request.getHeader("X-Request-Id"));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        LoginVO vo = authService.login(dto);
        return R.ok(vo).requestId(request.getHeader("X-Request-Id"));
    }

    /**
     * 退出登录，需要 Authorization 头
     */
    @PostMapping("/logout")
    public R<Boolean> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return R.ok(true).requestId(request.getHeader("X-Request-Id"));
        }
        String token = authHeader.substring(7);
        authService.logout(token);
        return R.ok(true).requestId(request.getHeader("X-Request-Id"));
    }

    /**
     * 刷新访问令牌
     */
    @PostMapping("/token/refresh")
    public R<RefreshTokenVO> refresh(@Valid @RequestBody RefreshTokenDTO dto, HttpServletRequest request) {
        RefreshTokenVO vo = authService.refreshToken(dto);
        return R.ok(vo).requestId(request.getHeader("X-Request-Id"));
    }
}
