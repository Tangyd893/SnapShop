package com.snapshop.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.product.entity.SkuStock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存 Mapper
 */
@Mapper
public interface SkuStockMapper extends BaseMapper<SkuStock> {
}
