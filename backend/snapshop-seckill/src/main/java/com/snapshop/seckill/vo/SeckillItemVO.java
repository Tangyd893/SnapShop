package com.snapshop.seckill.vo;

import lombok.Data;

/**
 * 秒杀活动商品 VO
 */
@Data
public class SeckillItemVO {

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

    /** 每人限购数量 */
    private Integer limitPerUser;

    /** 库存状态：有库存 / 已售罄 */
    private String stockStatus;
}
