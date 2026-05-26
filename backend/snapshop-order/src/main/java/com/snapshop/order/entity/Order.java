package com.snapshop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体，对应 order 表
 */
@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号，格式 SO+yyyyMMdd+6位随机数字 */
    private String orderNo;

    /** 用户编号 */
    private Long userId;

    /** 订单金额，单位分 */
    private Long totalAmount;

    /** 订单类型：NORMAL-普通订单，SECKILL-秒杀订单 */
    private String orderType;

    /** 订单状态：PENDING_PAY-待支付，PAID-已支付，CANCELLED-已取消 */
    private String status;

    /** 支付过期时间 */
    private LocalDateTime expireAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
