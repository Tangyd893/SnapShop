package com.snapshop.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀活动表实体
 */
@Data
@TableName("seckill_activity")
public class SeckillActivity {

    /** 活动编号 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动名称 */
    private String activityName;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 活动状态：NOT_STARTED / RUNNING / ENDED */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
