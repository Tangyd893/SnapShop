package com.snapshop.admin.vo;

import lombok.Data;

/**
 * Dashboard 统计数据 VO
 */
@Data
public class DashboardStatsVO {

    /** 今日订单数 */
    private Long todayOrderCount;

    /** 秒杀提交次数 */
    private Long todaySeckillSubmitCount;

    /** 秒杀成功率 */
    private String seckillSuccessRate;

    /** 死信消息数量 */
    private Long deadLetterCount;

    /** 商品总数 */
    private Long totalProductCount;
}
