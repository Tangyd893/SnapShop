package com.snapshop.auth.service;

import com.snapshop.auth.dto.LoginDTO;
import com.snapshop.auth.dto.RefreshTokenDTO;
import com.snapshop.auth.dto.RegisterDTO;
import com.snapshop.auth.vo.LoginVO;
import com.snapshop.auth.vo.RefreshTokenVO;
import com.snapshop.auth.vo.RegisterVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     */
    RegisterVO register(RegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 退出登录，将 token 加入黑名单
     */
    void logout(String token);

    /**
     * 使用刷新令牌换取新的访问令牌
     */
    RefreshTokenVO refreshToken(RefreshTokenDTO dto);
}
