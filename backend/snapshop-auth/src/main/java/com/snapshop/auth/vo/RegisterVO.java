package com.snapshop.auth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 注册响应 VO
 */
@Data
@Builder
public class RegisterVO {

    private Long userId;

    private String username;
}
