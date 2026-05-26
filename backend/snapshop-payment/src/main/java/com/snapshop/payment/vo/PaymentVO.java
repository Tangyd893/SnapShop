package com.snapshop.payment.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付单视图对象
 */
@Data
@Builder
public class PaymentVO {

    /** 支付单编号 */
    private Long paymentId;

    /** 订单编号 */
    private Long orderId;

    /** 支付金额，单位：分 */
    private Long payAmount;

    /** 支付状态 */
    private String payStatus;

    /** 支付时间 */
    private LocalDateTime paidAt;
}
