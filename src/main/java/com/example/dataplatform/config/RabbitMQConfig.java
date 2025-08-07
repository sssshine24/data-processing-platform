package com.example.dataplatform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_QUEUE = "order.queue";
    public static final String ORDER_ROUTING_KEY = "order.routingkey";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE, true);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(orderQueue()).to(orderExchange()).with(ORDER_ROUTING_KEY);
    }

    /**
     * 【核心修正】
     * 我们不再仅仅是创建一个空的 MessageConverter，
     * 而是完整地创建一个 ObjectMapper，手动为它注册上处理 Java 8 时间类型的模块，
     * 然后再用这个 ObjectMapper 去构造我们的 MessageConverter。
     * 这确保了无论 Spring Boot 自动配置出了什么问题，我们使用的转换器都是100%正确的。
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        // 1. 创建一个 Jackson 的 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // 2. 为 ObjectMapper 注册一个可以处理 LocalDateTime 等 Java 8 时间类型的模块
        objectMapper.registerModule(new JavaTimeModule());

        // 3. 使用这个配置好的 ObjectMapper 来创建我们的 JSON 消息转换器
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}