package com.snapshop.auth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应 VO
 */
@Data
@Builder
public class LoginVO {

    private String accessToken;

    private String refreshToken;

    private Integer expiresIn;

    private UserInfo user;
}
