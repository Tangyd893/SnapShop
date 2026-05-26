package com.snapshop.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.order.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地消息表 Mapper
 */
@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessage> {
}
