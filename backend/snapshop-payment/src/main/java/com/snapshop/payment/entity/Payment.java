package com.snapshop.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付单实体，对应 payment 表
 */
@Data
@TableName("payment")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支付单号，格式 PAY + yyyyMMddHHmmss + 4位随机数字 */
    private String paymentNo;

    /** 订单编号 */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 用户编号 */
    private Long userId;

    /** 支付金额，单位：分 */
    private Long payAmount;

    /** 支付方式：MOCK-模拟支付 */
    private String payType;

    /** 支付状态：PENDING_PAY-待支付，PAID-已支付 */
    private String payStatus;

    /** 支付时间 */
    private LocalDateTime paidAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
