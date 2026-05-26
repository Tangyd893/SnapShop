package com.snapshop.auth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 用户基础信息 VO
 */
@Data
@Builder
public class UserInfo {

    private Long userId;

    private String username;

    private String nickname;
}
