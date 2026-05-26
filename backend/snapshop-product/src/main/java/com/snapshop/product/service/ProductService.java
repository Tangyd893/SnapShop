package com.snapshop.product.service;

import com.snapshop.common.base.PageResult;
import com.snapshop.product.entity.ProductCategory;
import com.snapshop.product.vo.ProductDetailVO;
import com.snapshop.product.vo.ProductListVO;

import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 分页查询商品列表
     *
     * @param keyword    搜索关键词
     * @param categoryId 分类编号
     * @param pageNo     页码
     * @param pageSize   每页数量
     * @return 商品列表分页结果
     */
    PageResult<ProductListVO> getProductList(String keyword, Long categoryId, Integer pageNo, Integer pageSize);

    /**
     * 查询商品详情
     *
     * @param productId 商品编号
     * @return 商品详情
     */
    ProductDetailVO getProductDetail(Long productId);

    /**
     * 查询商品分类列表
     *
     * @return 分类列表
     */
    List<ProductCategory> getCategories();
}
