package com.snapshop.payment.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付成功消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息编号 */
    private String messageId;

    /** 支付单编号 */
    private Long paymentId;

    /** 订单编号 */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 支付金额，单位：分 */
    private Long paidAmount;

    /** 支付时间 */
    private LocalDateTime paidAt;
}
