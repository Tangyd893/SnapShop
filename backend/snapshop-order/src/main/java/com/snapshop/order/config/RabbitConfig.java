package com.snapshop.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 配置类
 */
@Configuration
public class RabbitConfig {

    /** 秒杀订单交换机 */
    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";

    /** 秒杀订单队列 */
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";

    /** 秒杀订单路由键 */
    public static final String SECKILL_ORDER_KEY = "seckill.order.create";

    /** 秒杀订单死信交换机 */
    public static final String SECKILL_ORDER_DEAD_EXCHANGE = "seckill.order.dead.exchange";

    /** 秒杀订单死信队列 */
    public static final String SECKILL_ORDER_DEAD_QUEUE = "seckill.order.dead.queue";

    /** 秒杀订单死信路由键 */
    public static final String SECKILL_ORDER_DEAD_KEY = "seckill.order.dead";

    /** 消费者组标识 */
    public static final String CONSUMER_GROUP = "snapshop-order";

    /**
     * 声明秒杀订单交换机（持久化、非自动删除）
     */
    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SECKILL_ORDER_EXCHANGE, true, false);
    }

    /**
     * 声明秒杀订单队列（持久化），绑定死信交换机
     */
    @Bean
    public Queue seckillOrderQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", SECKILL_ORDER_DEAD_EXCHANGE);
        args.put("x-dead-letter-routing-key", SECKILL_ORDER_DEAD_KEY);
        return QueueBuilder.durable(SECKILL_ORDER_QUEUE).withArguments(args).build();
    }

    /**
     * 声明秒杀订单死信交换机（持久化、非自动删除）
     */
    @Bean
    public DirectExchange seckillOrderDeadExchange() {
        return new DirectExchange(SECKILL_ORDER_DEAD_EXCHANGE, true, false);
    }

    /**
     * 声明秒杀订单死信队列（持久化）
     */
    @Bean
    public Queue seckillOrderDeadQueue() {
        return QueueBuilder.durable(SECKILL_ORDER_DEAD_QUEUE).build();
    }

    /**
     * 绑定：死信队列 → 死信交换机，路由键 seckill.order.dead
     */
    @Bean
    public Binding seckillOrderDeadBinding() {
        return BindingBuilder.bind(seckillOrderDeadQueue())
                .to(seckillOrderDeadExchange())
                .with(SECKILL_ORDER_DEAD_KEY);
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding seckillOrderBinding() {
        return BindingBuilder.bind(seckillOrderQueue()).to(seckillOrderExchange()).with(SECKILL_ORDER_KEY);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
