package com.snapshop.admin.service;

import com.snapshop.admin.entity.Product;
import com.snapshop.admin.entity.ProductCategory;
import com.snapshop.common.base.PageResult;

import java.util.List;

/**
 * 管理后台商品服务接口
 */
public interface ProductAdminService {

    /**
     * 分页查询商品列表
     */
    PageResult<Product> getProductList(String keyword, Long categoryId, Integer pageNo, Integer pageSize);

    /**
     * 创建商品
     */
    Product createProduct(Product product);

    /**
     * 更新商品
     */
    Product updateProduct(Long id, Product product);

    /**
     * 上下架商品
     */
    void updateProductStatus(Long id, String status);

    /**
     * 商品分类列表
     */
    List<ProductCategory> getCategories();

    /**
     * 创建商品分类
     */
    ProductCategory createCategory(ProductCategory category);
}
