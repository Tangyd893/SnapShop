package com.snapshop.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.seckill.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地消息表 Mapper
 */
@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessage> {
}
