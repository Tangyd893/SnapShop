package com.snapshop.seckill.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 秒杀令牌 VO
 */
@Data
@AllArgsConstructor
public class SeckillTokenVO {

    /** 秒杀令牌 */
    private String seckillToken;

    /** 过期时间，单位：秒 */
    private Integer expiresIn;
}
