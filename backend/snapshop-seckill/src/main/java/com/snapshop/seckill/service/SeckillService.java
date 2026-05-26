package com.snapshop.seckill.service;

import com.snapshop.common.base.PageResult;
import com.snapshop.seckill.vo.*;

/**
 * 秒杀服务接口
 */
public interface SeckillService {

    /**
     * 分页查询秒杀活动列表
     *
     * @param status   活动状态（可选）
     * @param pageNo   页码
     * @param pageSize 每页数量
     * @return 活动列表分页结果
     */
    PageResult<SeckillActivityListVO> getActivityList(String status, Integer pageNo, Integer pageSize);

    /**
     * 查询秒杀活动详情（含活动商品列表）
     *
     * @param activityId 活动编号
     * @return 活动详情
     */
    SeckillActivityDetailVO getActivityDetail(Long activityId);

    /**
     * 查询秒杀商品详情
     *
     * @param activityId 活动编号
     * @param skuId      商品规格编号
     * @param serverTime 服务器当前时间
     * @return 秒杀商品详情
     */
    SeckillItemDetailVO getSeckillItemDetail(Long activityId, Long skuId, String serverTime);

    /**
     * 生成秒杀令牌
     *
     * @param userId     用户编号
     * @param activityId 活动编号
     * @param skuId      商品规格编号
     * @return 秒杀令牌
     */
    SeckillTokenVO generateToken(Long userId, Long activityId, Long skuId);

    /**
     * 提交秒杀请求
     *
     * @param userId       用户编号
     * @param activityId   活动编号
     * @param skuId        商品规格编号
     * @param seckillToken 秒杀令牌
     * @param requestId    请求编号
     * @param quantity     数量
     * @return 秒杀提交结果
     */
    SeckillResultVO submitSeckill(Long userId, Long activityId, Long skuId,
                                  String seckillToken, String requestId, Integer quantity);

    /**
     * 查询秒杀结果
     *
     * @param requestId 请求编号
     * @return 秒杀结果
     */
    SeckillResultVO getSeckillResult(String requestId);

    /**
     * 库存预热
     *
     * @param activityId 活动编号
     */
    void warmUpStock(Long activityId);
}
