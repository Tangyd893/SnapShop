package com.snapshop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.order.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀订单关系 Mapper
 */
@Mapper
public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {
}
