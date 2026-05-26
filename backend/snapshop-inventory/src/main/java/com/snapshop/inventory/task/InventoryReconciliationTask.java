package com.snapshop.inventory.task;

import com.snapshop.inventory.entity.StockRecord;
import com.snapshop.inventory.mapper.SkuStockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 库存对账定时任务
 * 每天凌晨2点执行，对比 Redis 预扣库存与 MySQL 真实库存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryReconciliationTask {

    /** Redis 秒杀库存键前缀 */
    private static final String STOCK_KEY_PREFIX = "seckill:stock:";

    /** 差异告警阈值（超过此阈值记录警告日志） */
    private static final long DIFF_THRESHOLD = 5;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SkuStockMapper skuStockMapper;

    /**
     * 每天凌晨 2:00 执行库存对账
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void reconcileInventory() {
        log.info("========== 库存对账任务开始 ==========");

        try {
            // 获取 Redis 中所有秒杀库存键
            Set<String> stockKeys = redisTemplate.keys(STOCK_KEY_PREFIX + "*");
            if (stockKeys == null || stockKeys.isEmpty()) {
                log.info("未找到任何 Redis 秒杀库存键，对账结束");
                return;
            }

            log.info("找到 {} 个 Redis 秒杀库存键，开始逐一对比", stockKeys.size());

            int totalChecked = 0;
            int diffCount = 0;

            for (String key : stockKeys) {
                try {
                    // 从键名解析 skuId，键格式: seckill:stock:{activityId}:{skuId}
                    String[] parts = key.split(":");
                    if (parts.length < 4) {
                        log.warn("无法解析的 Redis 键: {}", key);
                        continue;
                    }

                    Long skuId = Long.parseLong(parts[3]);

                    // 读取 Redis 中的预扣库存
                    Object redisValue = redisTemplate.opsForValue().get(key);
                    int redisStock = 0;
                    if (redisValue instanceof Integer) {
                        redisStock = (Integer) redisValue;
                    } else if (redisValue instanceof Number) {
                        redisStock = ((Number) redisValue).intValue();
                    } else if (redisValue != null) {
                        redisStock = Integer.parseInt(redisValue.toString());
                    }

                    // 读取 MySQL 中的可用库存
                    StockRecord stockRecord = skuStockMapper.selectById(skuId);
                    if (stockRecord == null) {
                        log.warn("MySQL 中未找到库存记录: skuId={}, Redis库存={}", skuId, redisStock);
                        continue;
                    }

                    int mysqlStock = stockRecord.getAvailableStock();
                    long diff = redisStock - mysqlStock;

                    // 检查差异是否超过阈值
                    if (Math.abs(diff) > DIFF_THRESHOLD) {
                        log.warn("库存差异告警: key={}, skuId={}, Redis库存={}, MySQL库存={}, " +
                                "差异={}, 版本号={}",
                                key, skuId, redisStock, mysqlStock, diff, stockRecord.getVersion());
                        diffCount++;
                    }

                    totalChecked++;
                } catch (NumberFormatException e) {
                    log.warn("解析 skuId 失败: key={}, 错误={}", key, e.getMessage());
                }
            }

            log.info("========== 库存对账任务完成: 共检查 {} 条记录，发现 {} 条差异 ==========",
                    totalChecked, diffCount);

        } catch (Exception e) {
            log.error("库存对账任务执行异常", e);
        }
    }
}
