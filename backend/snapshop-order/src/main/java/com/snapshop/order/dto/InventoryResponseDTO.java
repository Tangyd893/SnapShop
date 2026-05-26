package com.snapshop.order.dto;

import lombok.Data;

/**
 * 库存扣减响应 DTO
 */
@Data
public class InventoryResponseDTO {

    /** 库存扣减是否成功 */
    private Boolean success;

    /** 商品规格编号 */
    private Long skuId;

    /** 扣减后剩余库存 */
    private Integer remainingStock;
}
