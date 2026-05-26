package com.snapshop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.order.entity.OrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态流水 Mapper
 */
@Mapper
public interface OrderStatusLogMapper extends BaseMapper<OrderStatusLog> {
}
