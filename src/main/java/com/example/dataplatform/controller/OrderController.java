package com.example.dataplatform.controller;

import com.example.dataplatform.config.RabbitMQConfig;
import com.example.dataplatform.model.Orders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostMapping("/submit")
    public String submitOrder(@RequestBody Orders order) {
        logger.info("接收到实时订单请求: {}", order.getOrderId());

        // 将订单消息发送到 RabbitMQ
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_ROUTING_KEY, order);

        logger.info("订单 [{}] 已成功发送到消息队列，等待异步处理。", order.getOrderId());

        // 立即返回，不等待数据库操作
        return "订单接收成功，正在后台处理中！";
    }
}