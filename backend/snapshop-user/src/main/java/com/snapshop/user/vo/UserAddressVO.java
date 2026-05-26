package com.snapshop.user.vo;

import lombok.Data;

/**
 * 用户地址 VO
 */
@Data
public class UserAddressVO {

    /** 地址编号 */
    private Long addressId;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号 */
    private String receiverPhone;

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 区/县 */
    private String district;

    /** 详细地址 */
    private String detailAddress;

    /** 是否为默认地址 */
    private Boolean defaultAddress;
}
