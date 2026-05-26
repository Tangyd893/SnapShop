package com.snapshop.admin.service;

import com.snapshop.admin.entity.SeckillActivity;
import com.snapshop.admin.entity.SeckillActivityItem;
import com.snapshop.common.base.PageResult;

/**
 * 管理后台秒杀活动服务接口
 */
public interface SeckillAdminService {

    /**
     * 分页查询秒杀活动列表
     */
    PageResult<SeckillActivity> getActivityList(Integer pageNo, Integer pageSize);

    /**
     * 创建秒杀活动
     */
    SeckillActivity createActivity(SeckillActivity activity);

    /**
     * 更新秒杀活动
     */
    SeckillActivity updateActivity(Long id, SeckillActivity activity);

    /**
     * 绑定秒杀商品到活动
     */
    SeckillActivityItem addActivityItem(Long activityId, SeckillActivityItem item);

    /**
     * 触发 Redis 预热
     */
    void warmUpStock(Long activityId);
}
