package com.snapshop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存扣减请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDeductDTO {

    /** 商品规格编号 */
    private Long skuId;

    /** 扣减数量 */
    private Integer quantity;

    /** 请求编号（用于幂等） */
    private String requestId;
}
