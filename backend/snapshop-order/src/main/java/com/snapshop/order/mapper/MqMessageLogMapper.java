package com.snapshop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.order.entity.MqMessageLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * MQ 消息消费日志 Mapper
 */
@Mapper
public interface MqMessageLogMapper extends BaseMapper<MqMessageLog> {
}
