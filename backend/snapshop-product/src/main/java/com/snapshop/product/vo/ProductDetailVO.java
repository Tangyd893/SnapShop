package com.snapshop.product.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品详情视图对象
 */
@Data
public class ProductDetailVO {

    /** 商品编号 */
    private Long productId;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 封面图地址 */
    private String coverUrl;

    /** 规格列表 */
    private List<SkuVO> skus;
}
