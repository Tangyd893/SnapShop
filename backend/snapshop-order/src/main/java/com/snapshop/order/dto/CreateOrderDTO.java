package com.snapshop.order.dto;

import lombok.Data;

/**
 * 创建订单请求 DTO
 */
@Data
public class CreateOrderDTO {

    /** 用户编号 */
    private Long userId;

    /** 商品规格编号 */
    private Long skuId;

    /** 商品标题 */
    private String title;

    /** 单价，单位分 */
    private Long price;

    /** 数量 */
    private Integer quantity;

    /** 活动编号（秒杀订单时传入） */
    private Long activityId;

    /** 请求编号（秒杀订单时传入） */
    private String requestId;

    /** 订单类型：NORMAL-普通订单，SECKILL-秒杀订单 */
    private String orderType;
}
