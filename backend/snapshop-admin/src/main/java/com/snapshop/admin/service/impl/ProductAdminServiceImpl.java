package com.snapshop.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snapshop.admin.entity.Product;
import com.snapshop.admin.entity.ProductCategory;
import com.snapshop.admin.mapper.ProductCategoryMapper;
import com.snapshop.admin.mapper.ProductMapper;
import com.snapshop.admin.service.ProductAdminService;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.PageResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductAdminServiceImpl implements ProductAdminService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private ProductCategoryMapper productCategoryMapper;

    @Override
    public PageResult<Product> getProductList(String keyword, Long categoryId, Integer pageNo, Integer pageSize) {
        int current = pageNo != null ? pageNo : 1;
        int size = pageSize != null ? pageSize : 10;

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getTitle, keyword);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> page = new Page<>(current, size);
        Page<Product> productPage = productMapper.selectPage(page, wrapper);

        return PageResult.of(productPage.getRecords(), productPage.getCurrent(),
                productPage.getSize(), productPage.getTotal());
    }

    @Override
    public Product createProduct(Product product) {
        productMapper.insert(product);
        return product;
    }

    @Override
    public Product updateProduct(Long id, Product product) {
        Product existing = productMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        product.setId(id);
        productMapper.updateById(product);
        return productMapper.selectById(id);
    }

    @Override
    public void updateProductStatus(Long id, String status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }
        product.setStatus(status);
        productMapper.updateById(product);
    }

    @Override
    public List<ProductCategory> getCategories() {
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ProductCategory::getSort);
        return productCategoryMapper.selectList(wrapper);
    }

    @Override
    public ProductCategory createCategory(ProductCategory category) {
        productCategoryMapper.insert(category);
        return category;
    }
}
