package com.snapshop.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存记录实体，对应 sku_stock 表
 */
@Data
@TableName("sku_stock")
public class StockRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品规格编号 */
    private Long skuId;

    /** 可用库存 */
    private Integer availableStock;

    /** 锁定库存（下单未支付） */
    private Integer lockedStock;

    /** 已售库存 */
    private Integer soldStock;

    /** 乐观锁版本号 */
    @Version
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
