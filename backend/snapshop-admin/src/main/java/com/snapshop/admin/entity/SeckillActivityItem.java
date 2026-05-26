package com.snapshop.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀活动商品表实体（管理后台直连）
 */
@Data
@TableName("seckill_activity_item")
public class SeckillActivityItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long skuId;

    private Long seckillPrice;

    private Integer activityStock;

    private Integer limitPerUser;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
