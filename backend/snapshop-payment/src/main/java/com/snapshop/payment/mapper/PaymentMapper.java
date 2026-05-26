package com.snapshop.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.payment.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付单 Mapper
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
