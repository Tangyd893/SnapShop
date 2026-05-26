package com.snapshop.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.snapshop.admin.entity.LocalMessage;
import com.snapshop.admin.entity.Order;
import com.snapshop.admin.entity.Product;
import com.snapshop.admin.entity.SeckillOrder;
import com.snapshop.admin.mapper.LocalMessageMapper;
import com.snapshop.admin.mapper.OrderMapper;
import com.snapshop.admin.mapper.ProductMapper;
import com.snapshop.admin.mapper.SeckillOrderMapper;
import com.snapshop.admin.service.DashboardService;
import com.snapshop.admin.vo.DashboardStatsVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private SeckillOrderMapper seckillOrderMapper;

    @Resource
    private LocalMessageMapper localMessageMapper;

    @Resource
    private ProductMapper productMapper;

    @Override
    public DashboardStatsVO getStats() {
        DashboardStatsVO vo = new DashboardStatsVO();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        // 今日订单数
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.between(Order::getCreatedAt, todayStart, todayEnd);
        vo.setTodayOrderCount(orderMapper.selectCount(orderWrapper));

        // 今日秒杀提交次数
        LambdaQueryWrapper<SeckillOrder> seckillWrapper = new LambdaQueryWrapper<>();
        seckillWrapper.between(SeckillOrder::getCreatedAt, todayStart, todayEnd);
        vo.setTodaySeckillSubmitCount(seckillOrderMapper.selectCount(seckillWrapper));

        // 死信数量
        LambdaQueryWrapper<LocalMessage> deadWrapper = new LambdaQueryWrapper<>();
        deadWrapper.eq(LocalMessage::getStatus, "SEND_FAILED");
        vo.setDeadLetterCount(localMessageMapper.selectCount(deadWrapper));

        // 商品总数
        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        vo.setTotalProductCount(productMapper.selectCount(productWrapper));

        // 秒杀成功率
        long submitCount = vo.getTodaySeckillSubmitCount() != null ? vo.getTodaySeckillSubmitCount() : 0;
        long orderCount = vo.getTodayOrderCount() != null ? vo.getTodayOrderCount() : 0;

        if (submitCount > 0) {
            double rate = (double) orderCount / submitCount * 100;
            vo.setSeckillSuccessRate(String.format("%.1f%%", rate));
        } else {
            vo.setSeckillSuccessRate("0.0%");
        }

        return vo;
    }
}
