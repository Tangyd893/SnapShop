package com.snapshop.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snapshop.admin.entity.SeckillActivity;
import com.snapshop.admin.entity.SeckillActivityItem;
import com.snapshop.admin.mapper.SeckillActivityItemMapper;
import com.snapshop.admin.mapper.SeckillActivityMapper;
import com.snapshop.admin.service.SeckillAdminService;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.common.base.PageResult;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SeckillAdminServiceImpl implements SeckillAdminService {

    private static final String SECKILL_STOCK_KEY_PREFIX = "seckill:stock:%d:%d";

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Resource
    private SeckillActivityItemMapper seckillActivityItemMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageResult<SeckillActivity> getActivityList(Integer pageNo, Integer pageSize) {
        int current = pageNo != null ? pageNo : 1;
        int size = pageSize != null ? pageSize : 10;

        LambdaQueryWrapper<SeckillActivity> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SeckillActivity::getCreatedAt);

        Page<SeckillActivity> page = new Page<>(current, size);
        Page<SeckillActivity> activityPage = seckillActivityMapper.selectPage(page, wrapper);

        return PageResult.of(activityPage.getRecords(), activityPage.getCurrent(),
                activityPage.getSize(), activityPage.getTotal());
    }

    @Override
    public SeckillActivity createActivity(SeckillActivity activity) {
        seckillActivityMapper.insert(activity);
        return activity;
    }

    @Override
    public SeckillActivity updateActivity(Long id, SeckillActivity activity) {
        SeckillActivity existing = seckillActivityMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "秒杀活动不存在");
        }
        activity.setId(id);
        seckillActivityMapper.updateById(activity);
        return seckillActivityMapper.selectById(id);
    }

    @Override
    public SeckillActivityItem addActivityItem(Long activityId, SeckillActivityItem item) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "秒杀活动不存在");
        }
        item.setActivityId(activityId);
        seckillActivityItemMapper.insert(item);
        return item;
    }

    @Override
    public void warmUpStock(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "秒杀活动不存在");
        }

        LambdaQueryWrapper<SeckillActivityItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillActivityItem::getActivityId, activityId)
                .eq(SeckillActivityItem::getStatus, "NORMAL");

        seckillActivityItemMapper.selectList(wrapper).forEach(item -> {
            String stockKey = String.format(SECKILL_STOCK_KEY_PREFIX, activityId, item.getSkuId());
            redisTemplate.opsForValue().set(stockKey, item.getActivityStock());
        });
    }
}
