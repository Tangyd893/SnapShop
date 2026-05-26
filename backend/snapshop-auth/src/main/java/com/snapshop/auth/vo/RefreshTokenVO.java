package com.snapshop.auth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 刷新令牌响应 VO
 */
@Data
@Builder
public class RefreshTokenVO {

    private String accessToken;

    private Integer expiresIn;
}
