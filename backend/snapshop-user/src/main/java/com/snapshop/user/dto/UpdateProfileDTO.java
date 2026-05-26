package com.snapshop.user.dto;

import lombok.Data;

/**
 * 更新用户资料 DTO
 */
@Data
public class UpdateProfileDTO {

    /** 昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatarUrl;
}
