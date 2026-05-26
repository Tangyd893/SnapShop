package com.snapshop.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态流水实体，对应 order_status_log 表
 */
@Data
@TableName("order_status_log")
public class OrderStatusLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号 */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 原状态 */
    private String fromStatus;

    /** 新状态 */
    private String toStatus;

    /** 变更原因 */
    private String reason;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
