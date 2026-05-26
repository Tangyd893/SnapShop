package com.snapshop.seckill.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秒杀商品详情 VO（单个商品详情页）
 */
@Data
public class SeckillItemDetailVO {

    /** 活动编号 */
    private Long activityId;

    /** 商品规格编号 */
    private Long skuId;

    /** 商品标题 */
    private String title;

    /** 封面图地址 */
    private String coverUrl;

    /** 原价，单位：分 */
    private Long originPrice;

    /** 秒杀价，单位：分 */
    private Long seckillPrice;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 每人限购数量 */
    private Integer limitPerUser;

    /** 活动状态 */
    private String status;

    /** 服务器当前时间 */
    private String serverTime;
}
