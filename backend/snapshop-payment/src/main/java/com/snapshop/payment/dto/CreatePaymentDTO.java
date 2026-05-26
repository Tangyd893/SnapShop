package com.snapshop.payment.dto;

import lombok.Data;

/**
 * 创建支付单请求 DTO
 */
@Data
public class CreatePaymentDTO {

    /** 支付方式，默认 MOCK（模拟支付） */
    private String payType = "MOCK";
}
