package com.snapshop.admin.controller;

import com.snapshop.admin.annotation.RequireRole;
import com.snapshop.admin.entity.Product;
import com.snapshop.admin.entity.ProductCategory;
import com.snapshop.admin.service.ProductAdminService;
import com.snapshop.common.base.PageResult;
import com.snapshop.common.base.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理后台商品控制器（T-1311 / T-1312）
 */
@RestController
@RequestMapping("/api/admin")
public class ProductAdminController {

    @Resource
    private ProductAdminService productAdminService;

    /**
     * 分页查询商品列表
     */
    @GetMapping("/products")
    public R<PageResult<Product>> getProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PageResult<Product> result = productAdminService.getProductList(keyword, categoryId, pageNo, pageSize);
        return R.ok(result);
    }

    /**
     * 创建商品
     */
    @PostMapping("/products")
    public R<Product> createProduct(@RequestBody Product product) {
        return R.ok(productAdminService.createProduct(product));
    }

    /**
     * 更新商品
     */
    @PutMapping("/products/{id}")
    public R<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return R.ok(productAdminService.updateProduct(id, product));
    }

    /**
     * 上下架商品
     */
    @PutMapping("/products/{id}/status")
    public R<Void> updateProductStatus(@PathVariable Long id, @RequestBody ProductStatusRequest request) {
        productAdminService.updateProductStatus(id, request.getStatus());
        return R.ok();
    }

    /**
     * 商品分类列表
     */
    @GetMapping("/categories")
    public R<List<ProductCategory>> getCategories() {
        return R.ok(productAdminService.getCategories());
    }

    /**
     * 创建商品分类
     */
    @PostMapping("/categories")
    public R<ProductCategory> createCategory(@RequestBody ProductCategory category) {
        return R.ok(productAdminService.createCategory(category));
    }

    /**
     * 上下架请求体
     */
    @lombok.Data
    public static class ProductStatusRequest {
        private String status;
    }
}
