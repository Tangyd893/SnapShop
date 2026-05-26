package com.snapshop.product.controller;

import com.snapshop.common.base.PageResult;
import com.snapshop.common.base.R;
import com.snapshop.product.entity.ProductCategory;
import com.snapshop.product.service.ProductService;
import com.snapshop.product.vo.ProductDetailVO;
import com.snapshop.product.vo.ProductListVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品控制器
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Resource
    private ProductService productService;

    /**
     * 分页查询商品列表
     *
     * @param keyword    搜索关键词
     * @param categoryId 分类编号
     * @param pageNo     页码，默认 1
     * @param pageSize   每页数量，默认 10
     * @return 商品列表分页结果
     */
    @GetMapping
    public R<PageResult<ProductListVO>> getProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PageResult<ProductListVO> result = productService.getProductList(keyword, categoryId, pageNo, pageSize);
        return R.ok(result);
    }

    /**
     * 查询商品详情
     *
     * @param productId 商品编号
     * @return 商品详情
     */
    @GetMapping("/{productId}")
    public R<ProductDetailVO> getProductDetail(@PathVariable Long productId) {
        ProductDetailVO vo = productService.getProductDetail(productId);
        return R.ok(vo);
    }

    /**
     * 查询商品分类列表
     *
     * @return 分类列表
     */
    @GetMapping("/categories")
    public R<List<ProductCategory>> getCategories() {
        List<ProductCategory> categories = productService.getCategories();
        return R.ok(categories);
    }
}
