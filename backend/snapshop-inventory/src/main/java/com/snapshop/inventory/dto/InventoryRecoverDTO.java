package com.snapshop.inventory.dto;

import lombok.Data;

/**
 * 库存回补请求 DTO
 */
@Data
public class InventoryRecoverDTO {

    /** 请求编号 */
    private String requestId;

    /** 业务幂等键 */
    private String businessKey;

    /** 商品规格编号 */
    private Long skuId;

    /** 回补数量 */
    private Integer quantity;

    /** 回补原因 */
    private String reason;
}
