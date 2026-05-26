package com.snapshop.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品表实体
 */
@Data
@TableName("product")
public class Product {

    /** 商品编号 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类编号 */
    private Long categoryId;

    /** 商品标题 */
    private String title;

    /** 商品描述 */
    private String description;

    /** 封面图地址 */
    private String coverUrl;

    /** 商品状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
