package com.snapshop.seckill.vo;

import lombok.Data;

/**
 * 秒杀结果 VO
 */
@Data
public class SeckillResultVO {

    /** 请求编号 */
    private String requestId;

    /** 结果状态：排队中 / 成功 / 失败 / 售罄 / 重复参与 / 结果不存在 */
    private String resultStatus;

    /** 订单编号 */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 失败原因 */
    private String failureReason;
}
