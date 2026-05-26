package com.snapshop.order.dto;

import lombok.Data;

/**
 * 库存回补请求 DTO（本地定义，与 inventory 模块 DTO 字段一致）
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
