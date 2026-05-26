package com.snapshop.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.snapshop.admin.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
