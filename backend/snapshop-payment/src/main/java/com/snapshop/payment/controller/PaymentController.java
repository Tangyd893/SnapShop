package com.snapshop.payment.controller;

import com.snapshop.common.base.R;
import com.snapshop.payment.dto.CreatePaymentDTO;
import com.snapshop.payment.entity.Payment;
import com.snapshop.payment.service.PaymentService;
import com.snapshop.payment.vo.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 支付服务接口
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 创建支付单（用户发起支付）
     *
     * @param orderId 订单编号
     * @param userId  用户编号（从请求头 X-User-Id 获取）
     * @param dto     支付方式请求
     * @return 支付单编号
     */
    @PostMapping("/orders/{orderId}")
    public R<Long> createPayment(@PathVariable Long orderId,
                                 @RequestHeader("X-User-Id") Long userId,
                                 @RequestBody(required = false) CreatePaymentDTO dto) {
        log.info("创建支付单: orderId={}, userId={}", orderId, userId);
        String payType = dto != null ? dto.getPayType() : "MOCK";
        Long paymentId = paymentService.createPayment(orderId, userId, payType);
        return R.ok(paymentId);
    }

    /**
     * 模拟支付成功
     *
     * @param paymentId 支付单编号
     * @return 操作结果
     */
    @PostMapping("/{paymentId}/success")
    public R<Boolean> mockPaySuccess(@PathVariable Long paymentId) {
        log.info("模拟支付成功: paymentId={}", paymentId);
        paymentService.mockPay(paymentId);
        return R.ok(true);
    }

    /**
     * 根据订单编号查询支付状态
     *
     * @param orderId 订单编号
     * @return 支付单信息
     */
    @GetMapping("/orders/{orderId}")
    public R<PaymentVO> getPaymentStatus(@PathVariable Long orderId) {
        log.info("查询支付状态: orderId={}", orderId);
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        if (payment == null) {
            return R.ok(null);
        }
        PaymentVO vo = PaymentVO.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .payAmount(payment.getPayAmount())
                .payStatus(payment.getPayStatus())
                .paidAt(payment.getPaidAt())
                .build();
        return R.ok(vo);
    }
}
