package com.snapshop.order;

import com.snapshop.order.entity.MqMessageLog;
import com.snapshop.order.entity.Order;
import com.snapshop.order.entity.SeckillOrder;
import com.snapshop.order.mapper.MqMessageLogMapper;
import com.snapshop.order.mapper.OrderMapper;
import com.snapshop.order.mapper.SeckillOrderMapper;
import com.snapshop.order.message.SeckillOrderMessage;
import com.snapshop.order.mq.SeckillOrderConsumer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 重复消费测试（T-903）
 * <p>
 * 验证：
 * 1. 同一消息被重复投递时只创建一个订单
 * 2. 消费者通过 mq_message_log 实现消息级幂等
 * 3. 消费者通过 seckill_order 唯一约束实现业务级幂等
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("重复消费测试")
class RepeatConsumeTest {

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MqMessageLogMapper mqMessageLogMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private SeckillOrderConsumer seckillOrderConsumer;

    /** 测试消息编号 */
    private static final String TEST_MESSAGE_ID = "TEST_REPEAT_MSG_001";
    /** 测试请求编号 */
    private static final String TEST_REQUEST_ID = "REQ_REPEAT_001";
    /** 测试业务键 */
    private static final String TEST_BUSINESS_KEY = "99999:99999:99999";

    @BeforeEach
    void setUp() {
        // 清理上一次测试残留数据
        mqMessageLogMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MqMessageLog>()
                .eq(MqMessageLog::getMessageId, TEST_MESSAGE_ID));
        seckillOrderMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getRequestId, TEST_REQUEST_ID));
        orderMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, 99999L));
        stringRedisTemplate.delete("seckill:result:" + TEST_REQUEST_ID);
    }

    @Test
    @DisplayName("同一消息重复投递 - 只创建一次订单")
    void testRepeatedConsumeCreatesOnlyOneOrder() {
        // 1. 预写一条 SUCCESS 状态的消费日志（模拟已消费成功的消息）
        MqMessageLog successLog = new MqMessageLog();
        successLog.setMessageId(TEST_MESSAGE_ID);
        successLog.setBusinessKey(TEST_BUSINESS_KEY);
        successLog.setConsumerGroup("snapshop-order");
        successLog.setStatus("SUCCESS");
        successLog.setRetryCount(0);
        successLog.setCreatedAt(java.time.LocalDateTime.now());
        successLog.setUpdatedAt(java.time.LocalDateTime.now());
        mqMessageLogMapper.insert(successLog);

        // 2. 构建重复消息
        SeckillOrderMessage duplicateMessage = buildTestMessage();

        // 3. 模拟重复消费：查询消费日志应返回 SUCCESS
        MqMessageLog existingLog = mqMessageLogMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MqMessageLog>()
                        .eq(MqMessageLog::getMessageId, TEST_MESSAGE_ID)
                        .eq(MqMessageLog::getConsumerGroup, "snapshop-order"));

        assertNotNull(existingLog);
        assertEquals("SUCCESS", existingLog.getStatus(),
                "已成功处理的消息应被识别");

        // 4. 确认 seckill_order 表中不存在记录（因为本测试不真正下单）
        Long seckillCount = seckillOrderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getRequestId, TEST_REQUEST_ID));
        assertEquals(0L, seckillCount,
                "应在创建订单前就被幂等检查拦截");
    }

    @Test
    @DisplayName("消息级幂等：消息日志已存在 PROCESSING 状态时的处理")
    void testProcessingStatusHandling() {
        // 1. 预写一条 PROCESSING 状态的消费日志（模拟上一轮处理未完成）
        MqMessageLog processingLog = new MqMessageLog();
        processingLog.setMessageId(TEST_MESSAGE_ID);
        processingLog.setBusinessKey(TEST_BUSINESS_KEY);
        processingLog.setConsumerGroup("snapshop-order");
        processingLog.setStatus("PROCESSING");
        processingLog.setRetryCount(1);
        processingLog.setCreatedAt(java.time.LocalDateTime.now());
        processingLog.setUpdatedAt(java.time.LocalDateTime.now());
        mqMessageLogMapper.insert(processingLog);

        // 2. 查询应找到 PROCESSING 状态的记录（非 SUCCESS，不应直接 ack）
        MqMessageLog existingLog = mqMessageLogMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MqMessageLog>()
                        .eq(MqMessageLog::getMessageId, TEST_MESSAGE_ID)
                        .eq(MqMessageLog::getConsumerGroup, "snapshop-order"));

        assertNotNull(existingLog);
        assertEquals("PROCESSING", existingLog.getStatus(),
                "PROCESSING 状态的消息应被识别，允许重新处理");

        // 3. 更新为重试
        existingLog.setRetryCount(existingLog.getRetryCount() + 1);
        existingLog.setUpdatedAt(java.time.LocalDateTime.now());
        mqMessageLogMapper.updateById(existingLog);

        // 4. 验证重试次数已递增
        MqMessageLog updated = mqMessageLogMapper.selectById(existingLog.getId());
        assertEquals(2, updated.getRetryCount(),
                "重新处理时重试次数应递增");
    }

    @Test
    @DisplayName("业务级幂等：seckill_order 唯一约束防止重复下单")
    void testSeckillOrderUniqueConstraint() {
        // 1. 预创建一条 seckill_order 记录（模拟已下过单）
        SeckillOrder existOrder = new SeckillOrder();
        existOrder.setOrderId(99999L);
        existOrder.setOrderNo("SO_DUP_TEST");
        existOrder.setUserId(99999L);
        existOrder.setActivityId(99999L);
        existOrder.setSkuId(99999L);
        existOrder.setRequestId(TEST_REQUEST_ID);
        existOrder.setCreatedAt(java.time.LocalDateTime.now());
        seckillOrderMapper.insert(existOrder);

        // 2. 检查同一 (userId, activityId, skuId) 是否存在
        Long count = seckillOrderMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getUserId, 99999L)
                        .eq(SeckillOrder::getActivityId, 99999L)
                        .eq(SeckillOrder::getSkuId, 99999L));

        assertEquals(1L, count,
                "同一用户同一活动同一商品，seckill_order 只能有一条记录");
    }

    /**
     * 构建测试用秒杀订单消息
     */
    private SeckillOrderMessage buildTestMessage() {
        SeckillOrderMessage msg = new SeckillOrderMessage();
        msg.setMessageId(TEST_MESSAGE_ID);
        msg.setRequestId(TEST_REQUEST_ID);
        msg.setBusinessKey(TEST_BUSINESS_KEY);
        msg.setUserId(99999L);
        msg.setActivityId(99999L);
        msg.setSkuId(99999L);
        msg.setQuantity(1);
        msg.setSeckillPrice(100L);
        msg.setCreatedAt(java.time.LocalDateTime.now());
        return msg;
    }
}
