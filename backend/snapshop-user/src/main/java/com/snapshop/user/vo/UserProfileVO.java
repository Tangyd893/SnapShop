package com.snapshop.user.vo;

import lombok.Data;

/**
 * 用户资料 VO
 */
@Data
public class UserProfileVO {

    /** 用户编号 */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 头像地址 */
    private String avatarUrl;

    /** 状态 */
    private Integer status;
}
