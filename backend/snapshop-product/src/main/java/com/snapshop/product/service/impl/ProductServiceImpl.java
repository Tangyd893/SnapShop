package com.snapshop.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.PageResult;
import com.snapshop.product.entity.Product;
import com.snapshop.product.entity.ProductCategory;
import com.snapshop.product.entity.Sku;
import com.snapshop.product.entity.SkuStock;
import com.snapshop.product.mapper.ProductCategoryMapper;
import com.snapshop.product.mapper.ProductMapper;
import com.snapshop.product.mapper.SkuMapper;
import com.snapshop.product.mapper.SkuStockMapper;
import com.snapshop.product.service.ProductService;
import com.snapshop.product.vo.ProductDetailVO;
import com.snapshop.product.vo.ProductListVO;
import com.snapshop.product.vo.SkuVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private SkuMapper skuMapper;

    @Resource
    private SkuStockMapper skuStockMapper;

    @Resource
    private ProductCategoryMapper productCategoryMapper;

    @Override
    public PageResult<ProductListVO> getProductList(String keyword, Long categoryId, Integer pageNo, Integer pageSize) {
        int current = pageNo != null ? pageNo : 1;
        int size = pageSize != null ? pageSize : 10;

        Page<Product> page = new Page<>(current, size);

        // 构建查询条件
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getTitle, keyword);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getCreatedAt);

        // 分页查询商品
        Page<Product> productPage = productMapper.selectPage(page, wrapper);
        List<Product> products = productPage.getRecords();

        // 组装 VO
        List<ProductListVO> voList = buildProductListVOList(products);

        return PageResult.of(voList, productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
    }

    @Override
    public ProductDetailVO getProductDetail(Long productId) {
        // 查询商品信息
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "商品不存在");
        }

        // 查询商品下的所有 SKU
        LambdaQueryWrapper<Sku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.eq(Sku::getProductId, productId)
                .orderByAsc(Sku::getId);
        List<Sku> skus = skuMapper.selectList(skuWrapper);

        // 组装 SKU VO 列表
        List<SkuVO> skuVOList = buildSkuVOList(skus);

        // 组装详情 VO
        ProductDetailVO vo = new ProductDetailVO();
        vo.setProductId(product.getId());
        vo.setTitle(product.getTitle());
        vo.setDescription(product.getDescription());
        vo.setCoverUrl(product.getCoverUrl());
        vo.setSkus(skuVOList);

        return vo;
    }

    @Override
    public List<ProductCategory> getCategories() {
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCategory::getStatus, "ON")
                .orderByAsc(ProductCategory::getSort);
        return productCategoryMapper.selectList(wrapper);
    }

    /**
     * 根据商品列表组装 ProductListVO 列表
     */
    private List<ProductListVO> buildProductListVOList(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有商品 ID
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        // 查询这些商品下的所有 SKU
        LambdaQueryWrapper<Sku> skuWrapper = new LambdaQueryWrapper<>();
        skuWrapper.in(Sku::getProductId, productIds)
                .orderByAsc(Sku::getProductId)
                .orderByAsc(Sku::getId);
        List<Sku> allSkus = skuMapper.selectList(skuWrapper);

        // 按 productId 分组，取每组第一个 SKU
        Map<Long, Sku> firstSkuMap = allSkus.stream()
                .collect(Collectors.toMap(
                        Sku::getProductId,
                        sku -> sku,
                        (existing, replacement) -> existing
                ));

        // 组装 VO 列表
        List<ProductListVO> voList = new ArrayList<>(products.size());
        for (Product product : products) {
            ProductListVO vo = new ProductListVO();
            vo.setProductId(product.getId());
            vo.setTitle(product.getTitle());
            vo.setCoverUrl(product.getCoverUrl());
            vo.setStatus(product.getStatus());

            // 设置第一个 SKU 的信息
            Sku firstSku = firstSkuMap.get(product.getId());
            if (firstSku != null) {
                vo.setSkuId(firstSku.getId());
                vo.setPrice(firstSku.getPrice());
            }

            voList.add(vo);
        }

        return voList;
    }

    /**
     * 根据 SKU 列表组装 SkuVO 列表，包含可用库存
     */
    private List<SkuVO> buildSkuVOList(List<Sku> skus) {
        if (skus == null || skus.isEmpty()) {
            return Collections.emptyList();
        }

        // 收集所有 SKU ID
        List<Long> skuIds = skus.stream()
                .map(Sku::getId)
                .collect(Collectors.toList());

        // 批量查询库存
        LambdaQueryWrapper<SkuStock> stockWrapper = new LambdaQueryWrapper<>();
        stockWrapper.in(SkuStock::getSkuId, skuIds);
        List<SkuStock> stocks = skuStockMapper.selectList(stockWrapper);

        // 按 skuId 索引库存
        Map<Long, Integer> stockMap = stocks.stream()
                .collect(Collectors.toMap(SkuStock::getSkuId, SkuStock::getAvailableStock));

        // 组装 VO 列表
        List<SkuVO> voList = new ArrayList<>(skus.size());
        for (Sku sku : skus) {
            SkuVO vo = new SkuVO();
            vo.setSkuId(sku.getId());
            vo.setSkuName(sku.getSkuName());
            vo.setPrice(sku.getPrice());
            vo.setStatus(sku.getStatus());
            // 从库存表获取可用库存，默认 0
            vo.setAvailableStock(stockMap.getOrDefault(sku.getId(), 0));

            voList.add(vo);
        }

        return voList;
    }
}
