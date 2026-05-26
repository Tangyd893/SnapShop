package com.snapshop.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.product.entity.Sku;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品规格 Mapper
 */
@Mapper
public interface SkuMapper extends BaseMapper<Sku> {
}
