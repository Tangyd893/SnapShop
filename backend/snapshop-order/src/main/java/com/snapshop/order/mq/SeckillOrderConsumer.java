package com.snapshop.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.snapshop.common.base.BizException;
import com.snapshop.common.base.ErrorCode;
import com.snapshop.order.config.RabbitConfig;
import com.snapshop.order.dto.CreateOrderDTO;
import com.snapshop.order.dto.InventoryDeductDTO;
import com.snapshop.order.dto.InventoryResponseDTO;
import com.snapshop.order.entity.*;
import com.snapshop.order.feign.InventoryFeignClient;
import com.snapshop.order.mapper.*;
import com.snapshop.order.message.SeckillOrderMessage;
import com.snapshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀订单 MQ 消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final OrderService orderService;
    private final SeckillOrderMapper seckillOrderMapper;
    private final MqMessageLogMapper mqMessageLogMapper;
    private final InventoryFeignClient inventoryFeignClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 秒杀结果 Redis 键前缀 */
    private static final String SECKILL_RESULT_PREFIX = "seckill:result:";
    /** 秒杀结果 TTL（秒） */
    private static final long SECKILL_RESULT_TTL = 86400L;
    /** 消费状态：处理中 */
    private static final String STATUS_PROCESSING = "PROCESSING";
    /** 消费状态：成功 */
    private static final String STATUS_SUCCESS = "SUCCESS";
    /** 消费状态：失败 */
    private static final String STATUS_FAILED = "FAILED";

    /**
     * 监听秒杀订单队列，手动确认模式
     */
    @RabbitListener(queues = RabbitConfig.SECKILL_ORDER_QUEUE)
    public void handleSeckillOrder(SeckillOrderMessage seckillMessage, Message message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String messageId = seckillMessage.getMessageId();
        log.info("收到秒杀订单消息: messageId={}, requestId={}, userId={}, skuId={}, activityId={}",
                messageId, seckillMessage.getRequestId(), seckillMessage.getUserId(),
                seckillMessage.getSkuId(), seckillMessage.getActivityId());

        try {
            // 1. 幂等性检查：查询 mq_message_log 表
            LambdaQueryWrapper<MqMessageLog> queryWrapper = new LambdaQueryWrapper<MqMessageLog>()
                    .eq(MqMessageLog::getMessageId, messageId)
                    .eq(MqMessageLog::getConsumerGroup, RabbitConfig.CONSUMER_GROUP);
            MqMessageLog existingLog = mqMessageLogMapper.selectOne(queryWrapper);

            if (existingLog != null && STATUS_SUCCESS.equals(existingLog.getStatus())) {
                log.info("消息已成功处理，直接确认: messageId={}", messageId);
                // 已成功处理，直接确认消息
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 插入 mq_message_log 记录（状态 = PROCESSING）
            MqMessageLog messageLog = new MqMessageLog();
            messageLog.setMessageId(messageId);
            messageLog.setBusinessKey(seckillMessage.getBusinessKey());
            messageLog.setConsumerGroup(RabbitConfig.CONSUMER_GROUP);
            messageLog.setStatus(STATUS_PROCESSING);
            messageLog.setRetryCount(0);
            messageLog.setCreatedAt(LocalDateTime.now());
            messageLog.setUpdatedAt(LocalDateTime.now());
            mqMessageLogMapper.insert(messageLog);

            // 3. 执行业务逻辑（事务包裹）
            processSeckillOrder(seckillMessage);

            // 4. 更新 mq_message_log 状态 = SUCCESS
            messageLog.setStatus(STATUS_SUCCESS);
            messageLog.setUpdatedAt(LocalDateTime.now());
            mqMessageLogMapper.updateById(messageLog);

            // 5. 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("秒杀订单处理成功: messageId={}, requestId={}", messageId, seckillMessage.getRequestId());

        } catch (Exception e) {
            log.error("秒杀订单处理失败: messageId={}, requestId={}, error={}",
                    messageId, seckillMessage.getRequestId(), e.getMessage(), e);

            try {
                // 更新 mq_message_log 状态 = FAILED
                updateMessageLogFailed(messageId, e);

                // 拒绝消息，重新入队
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("消息拒绝失败: messageId={}", messageId, ioException);
            }

            // 写入秒杀失败结果到 Redis
            writeSeckillResultFail(seckillMessage.getRequestId(), e.getMessage());
        }
    }

    /**
     * 处理秒杀订单核心逻辑（事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public void processSeckillOrder(SeckillOrderMessage seckillMessage) {
        Long userId = seckillMessage.getUserId();
        Long activityId = seckillMessage.getActivityId();
        Long skuId = seckillMessage.getSkuId();
        String requestId = seckillMessage.getRequestId();

        // 检查 seckill_order 表唯一约束 (user_id, activity_id, sku_id) 防止重复下单
        LambdaQueryWrapper<SeckillOrder> seckillQuery = new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getActivityId, activityId)
                .eq(SeckillOrder::getSkuId, skuId);
        Long count = seckillOrderMapper.selectCount(seckillQuery);
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.ALREADY_PARTICIPATED);
        }

        // 调用库存服务扣减库存
        InventoryDeductDTO deductDTO = new InventoryDeductDTO(skuId, seckillMessage.getQuantity(), requestId);
        R<InventoryResponseDTO> deductResult = inventoryFeignClient.deductStock(deductDTO);

        if (deductResult == null || deductResult.getCode() != 0 || deductResult.getData() == null
                || !Boolean.TRUE.equals(deductResult.getData().getSuccess())) {
            throw new BizException(ErrorCode.STOCK_DEDUCT_FAILED, "库存扣减失败");
        }

        // 创建订单
        CreateOrderDTO createOrderDTO = new CreateOrderDTO();
        createOrderDTO.setUserId(userId);
        createOrderDTO.setSkuId(skuId);
        createOrderDTO.setTitle("秒杀商品"); // 商品标题由秒杀服务预先设置
        createOrderDTO.setPrice(seckillMessage.getSeckillPrice());
        createOrderDTO.setQuantity(seckillMessage.getQuantity());
        createOrderDTO.setActivityId(activityId);
        createOrderDTO.setRequestId(requestId);
        createOrderDTO.setOrderType("SECKILL");

        Order order = orderService.createOrder(createOrderDTO);

        // 写入 Redis seckill:result:{requestId} = 成功结果 JSON
        writeSeckillResultSuccess(requestId, order.getId(), order.getOrderNo());
    }

    /**
     * 写入秒杀成功结果到 Redis
     */
    private void writeSeckillResultSuccess(String requestId, Long orderId, String orderNo) {
        try {
            String key = SECKILL_RESULT_PREFIX + requestId;
            String value = String.format(
                    "{\"requestId\":\"%s\",\"resultStatus\":\"SUCCESS\",\"orderId\":%d,\"orderNo\":\"%s\",\"failureReason\":null}",
                    requestId, orderId, orderNo);
            stringRedisTemplate.opsForValue().set(key, value, SECKILL_RESULT_TTL, TimeUnit.SECONDS);
            log.info("秒杀成功结果写入 Redis 成功: requestId={}", requestId);
        } catch (Exception e) {
            log.error("秒杀成功结果写入 Redis 失败: requestId={}", requestId, e);
        }
    }

    /**
     * 写入秒杀失败结果到 Redis
     */
    private void writeSeckillResultFail(String requestId, String failureReason) {
        if (requestId == null || requestId.isEmpty()) {
            return;
        }
        try {
            String key = SECKILL_RESULT_PREFIX + requestId;
            String reason = failureReason != null ? failureReason.replace("\"", "\\\"") : "系统异常";
            String value = String.format(
                    "{\"requestId\":\"%s\",\"resultStatus\":\"FAILED\",\"orderId\":null,\"orderNo\":null,\"failureReason\":\"%s\"}",
                    requestId, reason);
            stringRedisTemplate.opsForValue().set(key, value, SECKILL_RESULT_TTL, TimeUnit.SECONDS);
            log.info("秒杀失败结果写入 Redis 成功: requestId={}, reason={}", requestId, failureReason);
        } catch (Exception e) {
            log.error("秒杀失败结果写入 Redis 失败: requestId={}", requestId, e);
        }
    }

    /**
     * 更新消息日志状态为失败
     */
    private void updateMessageLogFailed(String messageId, Exception e) {
        try {
            LambdaQueryWrapper<MqMessageLog> queryWrapper = new LambdaQueryWrapper<MqMessageLog>()
                    .eq(MqMessageLog::getMessageId, messageId)
                    .eq(MqMessageLog::getConsumerGroup, RabbitConfig.CONSUMER_GROUP);
            MqMessageLog messageLog = mqMessageLogMapper.selectOne(queryWrapper);
            if (messageLog != null) {
                messageLog.setStatus(STATUS_FAILED);
                messageLog.setErrorMessage(e.getMessage());
                messageLog.setUpdatedAt(LocalDateTime.now());
                mqMessageLogMapper.updateById(messageLog);
            }
        } catch (Exception ex) {
            log.error("更新消息日志失败状态异常: messageId={}", messageId, ex);
        }
    }
}
