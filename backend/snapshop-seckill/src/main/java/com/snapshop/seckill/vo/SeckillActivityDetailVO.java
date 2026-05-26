package com.snapshop.seckill.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀活动详情 VO
 */
@Data
public class SeckillActivityDetailVO {

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

    /** 活动商品列表 */
    private List<SeckillItemVO> items;
}
