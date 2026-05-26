package com.snapshop.admin.service;

import com.snapshop.admin.vo.AdminLoginVO;

/**
 * 管理员用户服务接口
 */
public interface AdminUserService {

    /**
     * 管理员登录
     *
     * @param username 用户名
     * @param password 明文密码
     * @return 登录结果（含 JWT）
     */
    AdminLoginVO login(String username, String password);
}
