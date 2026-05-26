package com.snapshop.seckill.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.snapshop.seckill.entity.SeckillActivity;
import com.snapshop.seckill.mapper.SeckillActivityMapper;
import com.snapshop.seckill.service.SeckillService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 秒杀库存预热定时任务
 */
@Slf4j
@Component
public class SeckillWarmUpTask {

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Resource
    private SeckillService seckillService;

    /**
     * 服务启动时立即执行一次预热
     */
    @PostConstruct
    public void init() {
        log.info("服务启动，执行秒杀活动预热");
        doWarmUp();
    }

    /**
     * 每 30 秒扫描进行一次中的活动，执行库存预热
     */
    @Scheduled(fixedDelay = 30000)
    public void scheduledWarmUp() {
        doWarmUp();
    }

    /**
     * 扫描状态为 RUNNING 的活动，调用 warmUpStock
     */
    private void doWarmUp() {
        try {
            List<SeckillActivity> runningActivities = seckillActivityMapper.selectList(
                    new LambdaQueryWrapper<SeckillActivity>()
                            .eq(SeckillActivity::getStatus, "RUNNING"));

            if (runningActivities.isEmpty()) {
                return;
            }

            log.info("开始预热 {} 个进行中的秒杀活动", runningActivities.size());
            for (SeckillActivity activity : runningActivities) {
                try {
                    seckillService.warmUpStock(activity.getId());
                } catch (Exception e) {
                    log.error("活动预热失败，activityId={}", activity.getId(), e);
                }
            }
            log.info("秒杀活动预热完成");
        } catch (Exception e) {
            log.error("秒杀活动预热定时任务执行失败", e);
        }
    }
}
