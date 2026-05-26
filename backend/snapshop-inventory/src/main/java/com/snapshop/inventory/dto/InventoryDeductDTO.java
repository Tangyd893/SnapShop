package com.snapshop.inventory.dto;

import lombok.Data;

/**
 * 库存扣减请求 DTO
 */
@Data
public class InventoryDeductDTO {

    /** 请求编号 */
    private String requestId;

    /** 业务幂等键 */
    private String businessKey;

    /** 商品规格编号 */
    private Long skuId;

    /** 扣减数量 */
    private Integer quantity;

    /** 扣减场景 */
    private String scene;
}
