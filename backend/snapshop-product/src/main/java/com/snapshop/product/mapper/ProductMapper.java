package com.snapshop.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
