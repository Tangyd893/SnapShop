package com.snapshop.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.payment.config.RabbitConfig;
import com.snapshop.payment.entity.Payment;
import com.snapshop.payment.mapper.PaymentMapper;
import com.snapshop.payment.message.PaymentSuccessMessage;
import com.snapshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 支付服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final RabbitTemplate rabbitTemplate;

    /** 支付状态：待支付 */
    private static final String PAY_STATUS_PENDING = "PENDING_PAY";
    /** 支付状态：已支付 */
    private static final String PAY_STATUS_PAID = "PAID";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPayment(Long orderId, Long userId, String payType) {
        log.info("创建支付单: orderId={}, userId={}, payType={}", orderId, userId, payType);

        // 生成支付单号：PAY + yyyyMMddHHmmss + 4位随机数字
        String paymentNo = generatePaymentNo();

        // TODO: 调用订单服务查询订单获取订单号和金额
        // 当前使用模拟数据，实际应通过 Feign 调用订单服务
        String orderNo = "SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                ThreadLocalRandom.current().nextInt(100000, 999999);
        Long payAmount = 100L; // 模拟支付金额，单位：分

        LocalDateTime now = LocalDateTime.now();
        Payment payment = new Payment();
        payment.setPaymentNo(paymentNo);
        payment.setOrderId(orderId);
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setPayAmount(payAmount);
        payment.setPayType(payType != null ? payType : "MOCK");
        payment.setPayStatus(PAY_STATUS_PENDING);
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        paymentMapper.insert(payment);

        log.info("支付单创建成功: paymentId={}, paymentNo={}", payment.getId(), paymentNo);
        return payment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mockPay(Long paymentId) {
        log.info("模拟支付: paymentId={}", paymentId);

        // 查询支付单
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw new BizException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        // 校验支付状态
        if (PAY_STATUS_PAID.equals(payment.getPayStatus())) {
            throw new BizException(ErrorCode.PAYMENT_ALREADY_PAID);
        }

        // 更新支付状态
        LocalDateTime now = LocalDateTime.now();
        payment.setPayStatus(PAY_STATUS_PAID);
        payment.setPaidAt(now);
        payment.setUpdatedAt(now);
        paymentMapper.updateById(payment);

        // 发送支付成功消息到 RabbitMQ
        PaymentSuccessMessage message = new PaymentSuccessMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setPaymentId(payment.getId());
        message.setOrderId(payment.getOrderId());
        message.setOrderNo(payment.getOrderNo());
        message.setPaidAmount(payment.getPayAmount());
        message.setPaidAt(now);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.PAYMENT_EVENT_EXCHANGE,
                    RabbitConfig.PAYMENT_SUCCESS_KEY,
                    message);
            log.info("支付成功消息已发送: messageId={}, paymentId={}", message.getMessageId(), paymentId);
        } catch (Exception e) {
            log.error("支付成功消息发送失败: paymentId={}", paymentId, e);
            throw new BizException(ErrorCode.MESSAGE_SEND_FAILED, "支付成功消息发送失败");
        }
    }

    @Override
    public Payment getPaymentByOrderId(Long orderId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId);
        return paymentMapper.selectOne(wrapper);
    }

    /**
     * 生成支付单号，格式：PAY + yyyyMMddHHmmss + 4位随机数字
     */
    private String generatePaymentNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomNum = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PAY" + dateStr + randomNum;
    }
}
