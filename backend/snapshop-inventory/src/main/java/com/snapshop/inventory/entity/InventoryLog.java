package com.snapshop.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存流水实体，对应 inventory_log 表
 */
@Data
@TableName("inventory_log")
public class InventoryLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务幂等键 */
    private String businessKey;

    /** 商品规格编号 */
    private Long skuId;

    /** 变更类型：DEDUCT-扣减，RECOVER-回补 */
    private String changeType;

    /** 变更数量 */
    private Integer quantity;

    /** 变更前可用库存 */
    private Integer beforeAvailableStock;

    /** 变更后可用库存 */
    private Integer afterAvailableStock;

    /** 变更原因 */
    private String reason;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
