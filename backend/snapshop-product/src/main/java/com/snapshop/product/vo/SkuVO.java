package com.snapshop.product.vo;

import lombok.Data;

/**
 * 规格视图对象
 */
@Data
public class SkuVO {

    /** 规格编号 */
    private Long skuId;

    /** 规格名称 */
    private String skuName;

    /** 售价，单位：分 */
    private Long price;

    /** 可用库存 */
    private Integer availableStock;

    /** 规格状态 */
    private String status;
}
