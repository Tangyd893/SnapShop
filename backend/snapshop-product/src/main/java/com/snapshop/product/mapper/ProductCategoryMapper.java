package com.snapshop.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.product.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类 Mapper
 */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
