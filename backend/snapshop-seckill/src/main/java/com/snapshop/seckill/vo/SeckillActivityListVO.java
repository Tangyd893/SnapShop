package com.snapshop.seckill.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀活动列表 VO
 */
@Data
public class SeckillActivityListVO {

    /** 活动编号 */
    private Long activityId;

    /** 活动名称 */
    private String activityName;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 活动状态 */
    private String status;
}
