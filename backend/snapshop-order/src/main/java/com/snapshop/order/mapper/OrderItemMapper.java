package com.snapshop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
