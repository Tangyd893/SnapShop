package com.snapshop.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库存扣减请求 DTO，字段对齐 snapshop-inventory 侧同名 DTO 以支持 Feign JSON 传参
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDeductDTO {

    /** 商品规格编号 */
    private Long skuId;

    /** 扣减数量 */
    private Integer quantity;

    /** 请求编号（用于幂等，必填） */
    private String requestId;

    /** 业务幂等键（必填，如 userId:activityId:skuId） */
    private String businessKey;

    /** 扣减场景 */
    private String scene;

    public InventoryDeductDTO(Long skuId, Integer quantity, String requestId) {
        this.skuId = skuId;
        this.quantity = quantity;
        this.requestId = requestId;
    }
}
