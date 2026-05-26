package com.snapshop.order.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀订单消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息编号（全局唯一） */
    private String messageId;

    /** 请求编号（追踪链路） */
    private String requestId;

    /** 业务键（秒杀订单唯一性校验） */
    private String businessKey;

    /** 用户编号 */
    private Long userId;

    /** 活动编号 */
    private Long activityId;

    /** 商品规格编号 */
    private Long skuId;

    /** 购买数量 */
    private Integer quantity;

    /** 秒杀价格（单位：分） */
    private Long seckillPrice;

    /** 消息创建时间 */
    private LocalDateTime createdAt;
}
