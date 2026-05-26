package com.snapshop.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品库存表实体（管理后台直连）
 */
@Data
@TableName("sku_stock")
public class SkuStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long skuId;

    private Integer availableStock;

    private Integer lockedStock;

    private Integer soldStock;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
