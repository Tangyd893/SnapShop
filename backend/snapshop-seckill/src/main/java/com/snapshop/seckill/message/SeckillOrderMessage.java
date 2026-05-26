package com.snapshop.seckill.message;

import lombok.Data;

/**
 * 秒杀下单消息体
 */
@Data
public class SeckillOrderMessage {

    /** 消息编号，格式：秒杀消息:{activityId}:{skuId}:{userId}:{requestId} */
    private String messageId;

    /** 请求编号 */
    private String requestId;

    /** 业务幂等键，格式：{userId}:{activityId}:{skuId} */
    private String businessKey;

    /** 用户编号 */
    private Long userId;

    /** 活动编号 */
    private Long activityId;

    /** 商品规格编号 */
    private Long skuId;

    /** 数量 */
    private Integer quantity;

    /** 秒杀价格，单位：分 */
    private Long seckillPrice;

    /** 消息创建时间 */
    private String createdAt;
}
