package com.snapshop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品规格表实体
 */
@Data
@TableName("sku")
public class Sku {

    /** 规格编号 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品编号 */
    private Long productId;

    /** 规格名称 */
    private String skuName;

    /** 售价，单位：分 */
    private Long price;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
