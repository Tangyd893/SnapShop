package com.snapshop.admin.vo;

import lombok.Data;

/**
 * 管理员登录响应 VO
 */
@Data
public class AdminLoginVO {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private Long adminId;
    private String username;
    private String role;

    public static AdminLoginVO of(String token, long expiresIn, Long adminId, String username, String role) {
        AdminLoginVO vo = new AdminLoginVO();
        vo.accessToken = token;
        vo.tokenType = "Bearer";
        vo.expiresIn = expiresIn;
        vo.adminId = adminId;
        vo.username = username;
        vo.role = role;
        return vo;
    }
}
