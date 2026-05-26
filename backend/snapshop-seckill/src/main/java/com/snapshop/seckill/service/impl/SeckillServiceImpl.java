package com.snapshop.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapshop.common.base.*;
import com.snapshop.seckill.config.RabbitConfig;
import com.snapshop.seckill.entity.LocalMessage;
import com.snapshop.seckill.entity.SeckillActivity;
import com.snapshop.seckill.entity.SeckillActivityItem;
import com.snapshop.seckill.mapper.LocalMessageMapper;
import com.snapshop.seckill.mapper.SeckillActivityItemMapper;
import com.snapshop.seckill.mapper.SeckillActivityMapper;
import com.snapshop.seckill.message.SeckillOrderMessage;
import com.snapshop.seckill.service.SeckillService;
import com.snapshop.seckill.vo.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 秒杀服务实现
 */
@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String USER_KEY_PREFIX = "seckill:user:";
    private static final String TOKEN_KEY_PREFIX = "seckill:token:";
    private static final String RESULT_KEY_PREFIX = "seckill:result:";
    private static final String ACTIVITY_KEY_PREFIX = "seckill:activity:";
    private static final String ITEM_KEY_PREFIX = "seckill:item:";

    /** 活动状态常量 */
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String STATUS_ENDED = "ENDED";

    /** 库存状态常量 */
    private static final String STOCK_AVAILABLE = "有库存";
    private static final String STOCK_SOLD_OUT = "已售罄";

    /** 消息状态常量 */
    private static final String MSG_STATUS_PENDING = "待发送";

    @Resource
    private SeckillActivityMapper seckillActivityMapper;

    @Resource
    private SeckillActivityItemMapper seckillActivityItemMapper;

    @Resource
    private LocalMessageMapper localMessageMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private DefaultRedisScript<Long> seckillScript;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public PageResult<SeckillActivityListVO> getActivityList(String status, Integer pageNo, Integer pageSize) {
        LambdaQueryWrapper<SeckillActivity> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(SeckillActivity::getStatus, status);
        }
        queryWrapper.orderByDesc(SeckillActivity::getStartTime);

        Page<SeckillActivity> page = new Page<>(pageNo, pageSize);
        Page<SeckillActivity> resultPage = seckillActivityMapper.selectPage(page, queryWrapper);

        List<SeckillActivityListVO> records = resultPage.getRecords().stream().map(activity -> {
            SeckillActivityListVO vo = new SeckillActivityListVO();
            vo.setActivityId(activity.getId());
            vo.setActivityName(activity.getActivityName());
            vo.setStartTime(activity.getStartTime());
            vo.setEndTime(activity.getEndTime());
            vo.setStatus(activity.getStatus());
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(records, resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    @Override
    public SeckillActivityDetailVO getActivityDetail(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }

        List<SeckillItemVO> items = seckillActivityItemMapper.selectItemsWithProduct(activityId);
        for (SeckillItemVO item : items) {
            item.setStockStatus(getStockStatus(activityId, item.getSkuId()));
        }

        SeckillActivityDetailVO vo = new SeckillActivityDetailVO();
        vo.setActivityId(activity.getId());
        vo.setActivityName(activity.getActivityName());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStatus(activity.getStatus());
        vo.setItems(items);
        return vo;
    }

    @Override
    public SeckillItemDetailVO getSeckillItemDetail(Long activityId, Long skuId, String serverTime) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }

        SeckillItemVO item = seckillActivityItemMapper.selectItemWithProduct(activityId, skuId);
        if (item == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }

        SeckillItemDetailVO vo = new SeckillItemDetailVO();
        vo.setActivityId(activityId);
        vo.setSkuId(skuId);
        vo.setTitle(item.getTitle());
        vo.setCoverUrl(item.getCoverUrl());
        vo.setOriginPrice(item.getOriginPrice());
        vo.setSeckillPrice(item.getSeckillPrice());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setLimitPerUser(item.getLimitPerUser());
        vo.setStatus(activity.getStatus());
        vo.setServerTime(serverTime != null ? serverTime : LocalDateTime.now().format(FORMATTER));
        return vo;
    }

    @Override
    public SeckillTokenVO generateToken(Long userId, Long activityId, Long skuId) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        if (!STATUS_RUNNING.equals(activity.getStatus())) {
            validateActivityTime(activity);
        }

        String userKey = USER_KEY_PREFIX + activityId + ":" + skuId + ":" + userId;
        Boolean participated = stringRedisTemplate.hasKey(userKey);
        if (Boolean.TRUE.equals(participated)) {
            throw new BizException(ErrorCode.ALREADY_PARTICIPATED);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        String tokenKey = TOKEN_KEY_PREFIX + token;
        String tokenValue = userId + ":" + activityId + ":" + skuId;
        stringRedisTemplate.opsForValue().set(tokenKey, tokenValue, Duration.ofSeconds(60));

        return new SeckillTokenVO(token, 60);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillResultVO submitSeckill(Long userId, Long activityId, Long skuId,
                                         String seckillToken, String requestId, Integer quantity) {
        if (requestId == null || requestId.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请求编号不能为空");
        }

        // 1. 校验秒杀令牌
        String tokenKey = TOKEN_KEY_PREFIX + seckillToken;
        String tokenValue = (String) stringRedisTemplate.opsForValue().get(tokenKey);
        if (tokenValue == null || tokenValue.isEmpty()) {
            throw new BizException(ErrorCode.SECKILL_TOKEN_INVALID);
        }
        // 一次性使用，删除令牌
        stringRedisTemplate.delete(tokenKey);

        // 2. 从令牌值解析 userId, activityId, skuId
        String[] parts = tokenValue.split(":");
        if (parts.length != 3) {
            throw new BizException(ErrorCode.SECKILL_TOKEN_INVALID);
        }
        Long tokenUserId = Long.parseLong(parts[0]);
        Long tokenActivityId = Long.parseLong(parts[1]);
        Long tokenSkuId = Long.parseLong(parts[2]);

        // 3. 验证活动状态和时间
        SeckillActivity activity = seckillActivityMapper.selectById(tokenActivityId);
        if (activity == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        validateActivityTime(activity);

        // 4. 验证用户身份一致性
        if (!tokenUserId.equals(userId)) {
            throw new BizException(ErrorCode.SECKILL_TOKEN_INVALID);
        }

        // 5. 执行 Redis Lua 脚本原子扣减库存
        String stockKey = STOCK_KEY_PREFIX + tokenActivityId + ":" + tokenSkuId;
        String userKey = USER_KEY_PREFIX + tokenActivityId + ":" + tokenSkuId + ":" + userId;

        Long result = stringRedisTemplate.execute(
                seckillScript,
                List.of(stockKey, userKey),
                String.valueOf(userId)
        );

        if (result == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "Lua 脚本执行异常");
        }

        SeckillResultVO resultVO = new SeckillResultVO();
        resultVO.setRequestId(requestId);

        if (result == 0L) {
            // 库存不足 / 售罄
            resultVO.setResultStatus("售罄");
            throw new BizException(ErrorCode.SOLD_OUT);
        } else if (result == 1L) {
            // 用户已参与
            resultVO.setResultStatus("重复参与");
            throw new BizException(ErrorCode.ALREADY_PARTICIPATED);
        }

        // 6. 预扣成功，构造消息，写入本地消息表，发送 RabbitMQ
        String messageId = "秒杀消息:" + tokenActivityId + ":" + tokenSkuId + ":" + userId + ":" + requestId;
        String businessKey = userId + ":" + tokenActivityId + ":" + tokenSkuId;
        String createdAt = LocalDateTime.now().format(FORMATTER);

        SeckillOrderMessage orderMessage = new SeckillOrderMessage();
        orderMessage.setMessageId(messageId);
        orderMessage.setRequestId(requestId);
        orderMessage.setBusinessKey(businessKey);
        orderMessage.setUserId(userId);
        orderMessage.setActivityId(tokenActivityId);
        orderMessage.setSkuId(tokenSkuId);
        orderMessage.setQuantity(quantity != null ? quantity : 1);
        orderMessage.setCreatedAt(createdAt);

        // 秒杀价格：优先从 Redis 缓存读取，否则从数据库查
        Long seckillPrice = getSeckillPriceFromCache(tokenActivityId, tokenSkuId);
        orderMessage.setSeckillPrice(seckillPrice);

        // 写入本地消息表
        String payload;
        try {
            payload = objectMapper.writeValueAsString(orderMessage);
        } catch (JsonProcessingException e) {
            log.error("消息序列化失败", e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "消息序列化失败");
        }

        LocalMessage localMessage = new LocalMessage();
        localMessage.setMessageId(messageId);
        localMessage.setExchangeName(RabbitConfig.SECKILL_ORDER_EXCHANGE);
        localMessage.setRoutingKey(RabbitConfig.SECKILL_ORDER_ROUTING_KEY);
        localMessage.setPayload(payload);
        localMessage.setStatus(MSG_STATUS_PENDING);
        localMessage.setRetryCount(0);
        localMessage.setCreatedAt(LocalDateTime.now());
        localMessage.setUpdatedAt(LocalDateTime.now());
        localMessageMapper.insert(localMessage);

        // 发送到 RabbitMQ（带发布确认）
        CorrelationData correlationData = new CorrelationData(messageId);
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.SECKILL_ORDER_EXCHANGE,
                    RabbitConfig.SECKILL_ORDER_ROUTING_KEY,
                    orderMessage,
                    correlationData
            );
            log.info("秒杀下单消息已发送，messageId={}, requestId={}", messageId, requestId);
        } catch (Exception e) {
            log.error("消息发送异常，messageId={}", messageId, e);
            // 消息状态保持"待发送"，后续定时任务会重试
            throw new BizException(ErrorCode.MESSAGE_SEND_FAILED);
        }

        resultVO.setResultStatus("排队中");
        return resultVO;
    }

    @Override
    public SeckillResultVO getSeckillResult(String requestId) {
        String resultKey = RESULT_KEY_PREFIX + requestId;
        String json = (String) stringRedisTemplate.opsForValue().get(resultKey);
        if (json == null || json.isEmpty()) {
            SeckillResultVO vo = new SeckillResultVO();
            vo.setRequestId(requestId);
            vo.setResultStatus("结果不存在");
            throw new BizException(ErrorCode.SECKILL_RESULT_EXPIRED);
        }

        try {
            return objectMapper.readValue(json, SeckillResultVO.class);
        } catch (JsonProcessingException e) {
            log.error("秒杀结果解析失败，requestId={}", requestId, e);
            throw new BizException(ErrorCode.INTERNAL_ERROR, "结果解析失败");
        }
    }

    @Override
    public void warmUpStock(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            log.warn("预热库存失败：活动不存在，activityId={}", activityId);
            return;
        }

        // 活动信息写入 Redis
        String activityKey = ACTIVITY_KEY_PREFIX + activityId;
        try {
            stringRedisTemplate.opsForValue().set(activityKey, objectMapper.writeValueAsString(activity),
                    getActivityRemainingSeconds(activity), TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.error("活动信息序列化失败", e);
        }

        // 查询活动商品列表
        List<SeckillActivityItem> items = seckillActivityItemMapper.selectList(
                new LambdaQueryWrapper<SeckillActivityItem>().eq(SeckillActivityItem::getActivityId, activityId));

        for (SeckillActivityItem item : items) {
            // 活动商品信息写入 Redis
            String itemKey = ITEM_KEY_PREFIX + activityId + ":" + item.getSkuId();
            try {
                stringRedisTemplate.opsForValue().set(itemKey, objectMapper.writeValueAsString(item),
                        getActivityRemainingSeconds(activity), TimeUnit.SECONDS);
            } catch (JsonProcessingException e) {
                log.error("活动商品信息序列化失败", e);
            }

            // 活动库存写入 Redis
            String stockKey = STOCK_KEY_PREFIX + activityId + ":" + item.getSkuId();
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(item.getActivityStock()),
                    getActivityRemainingSeconds(activity), TimeUnit.SECONDS);
        }

        log.info("秒杀活动预热完成，activityId={}, 商品数量={}", activityId, items.size());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 校验活动时间，未开始或已结束抛异常
     */
    private void validateActivityTime(SeckillActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BizException(ErrorCode.SECKILL_NOT_STARTED);
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BizException(ErrorCode.SECKILL_ENDED);
        }
    }

    /**
     * 获取库存状态
     */
    private String getStockStatus(Long activityId, Long skuId) {
        String stockKey = STOCK_KEY_PREFIX + activityId + ":" + skuId;
        String stock = (String) stringRedisTemplate.opsForValue().get(stockKey);
        if (stock == null || "0".equals(stock)) {
            return STOCK_SOLD_OUT;
        }
        try {
            return Integer.parseInt(stock) > 0 ? STOCK_AVAILABLE : STOCK_SOLD_OUT;
        } catch (NumberFormatException e) {
            return STOCK_SOLD_OUT;
        }
    }

    /**
     * 从 Redis 缓存获取秒杀价格，缓存未命中时从数据库查询
     */
    private Long getSeckillPriceFromCache(Long activityId, Long skuId) {
        String itemKey = ITEM_KEY_PREFIX + activityId + ":" + skuId;
        String json = (String) stringRedisTemplate.opsForValue().get(itemKey);
        if (json != null) {
            try {
                SeckillActivityItem item = objectMapper.readValue(json, SeckillActivityItem.class);
                return item.getSeckillPrice();
            } catch (JsonProcessingException ignored) {
            }
        }

        // 缓存未命中，查数据库
        SeckillActivityItem item = seckillActivityItemMapper.selectOne(
                new LambdaQueryWrapper<SeckillActivityItem>()
                        .eq(SeckillActivityItem::getActivityId, activityId)
                        .eq(SeckillActivityItem::getSkuId, skuId));
        return item != null ? item.getSeckillPrice() : 0L;
    }

    /**
     * 计算活动剩余有效秒数，用于 Redis key TTL
     */
    private long getActivityRemainingSeconds(SeckillActivity activity) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(activity.getEndTime())) {
            return 3600; // 活动已结束，保留1小时
        }
        return Duration.between(now, activity.getEndTime()).getSeconds() + 3600; // 结束后多存1小时
    }
}
