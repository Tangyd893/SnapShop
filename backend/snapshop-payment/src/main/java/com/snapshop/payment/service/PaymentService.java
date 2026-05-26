package com.snapshop.payment.service;

import com.snapshop.payment.entity.Payment;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付单
     *
     * @param orderId 订单编号
     * @param userId  用户编号
     * @param payType 支付方式
     * @return 支付单编号
     */
    Long createPayment(Long orderId, Long userId, String payType);

    /**
     * 模拟支付成功
     *
     * @param paymentId 支付单编号
     */
    void mockPay(Long paymentId);

    /**
     * 根据订单编号查询支付单
     *
     * @param orderId 订单编号
     * @return 支付单
     */
    Payment getPaymentByOrderId(Long orderId);
}
