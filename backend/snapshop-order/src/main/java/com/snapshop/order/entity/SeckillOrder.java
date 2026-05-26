package com.snapshop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀订单关系实体，对应 seckill_order 表
 */
@Data
@TableName("seckill_order")
public class SeckillOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号 */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 用户编号 */
    private Long userId;

    /** 活动编号 */
    private Long activityId;

    /** 商品规格编号 */
    private Long skuId;

    /** 请求编号 */
    private String requestId;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
