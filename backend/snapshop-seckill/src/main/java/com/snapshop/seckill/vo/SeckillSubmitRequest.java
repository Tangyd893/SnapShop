package com.snapshop.seckill.vo;

import lombok.Data;

/**
 * 秒杀提交请求体
 */
@Data
public class SeckillSubmitRequest {

    /** 秒杀令牌 */
    private String seckillToken;

    /** 数量，默认 1 */
    private Integer quantity;
}
