package com.snapshop.product.vo;

import lombok.Data;

/**
 * 商品列表视图对象
 */
@Data
public class ProductListVO {

    /** 商品编号 */
    private Long productId;

    /** 默认规格编号（该商品下第一个 SKU） */
    private Long skuId;

    /** 商品标题 */
    private String title;

    /** 封面图地址 */
    private String coverUrl;

    /** 售价，单位：分 */
    private Long price;

    /** 商品状态 */
    private String status;
}
