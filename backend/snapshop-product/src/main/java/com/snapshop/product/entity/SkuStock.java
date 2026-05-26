package com.snapshop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品库存表实体
 */
@Data
@TableName("sku_stock")
public class SkuStock {

    /** 库存记录编号 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规格编号 */
    private Long skuId;

    /** 可用库存 */
    private Integer availableStock;

    /** 锁定库存 */
    private Integer lockedStock;

    /** 已售库存 */
    private Integer soldStock;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
