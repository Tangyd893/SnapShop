package com.snapshop.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀活动商品表实体
 */
@Data
@TableName("seckill_activity_item")
public class SeckillActivityItem {

    /** 活动商品编号 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动编号 */
    private Long activityId;

    /** 商品规格编号 */
    private Long skuId;

    /** 秒杀价，单位：分 */
    private Long seckillPrice;

    /** 活动库存 */
    private Integer activityStock;

    /** 每人限购数量 */
    private Integer limitPerUser;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
