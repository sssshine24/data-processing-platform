package com.example.dataplatform.service;

import com.example.dataplatform.config.RabbitMQConfig;
import com.example.dataplatform.mapper.OrdersChannelAMapper;
import com.example.dataplatform.model.Orders;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
public class OrderConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(OrderConsumerService.class);

    @Resource
    private OrdersChannelAMapper ordersChannelAMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void receiveOrderMessage(Orders order, Message message, Channel channel) throws IOException {
        logger.info("从消息队列中接收到订单 [{}]，准备写入数据库...", order.getOrderId());
        try {
            // 设置要插入的表名，并执行数据库插入
            order.setDynamicTableName("orders_channel_a"); // 我们统一存入A表
            ordersChannelAMapper.insertDynamic(order);

            logger.info("订单 [{}] 已成功写入数据库！", order.getOrderId());

            // 手动确认消息已被成功消费
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            logger.error("处理订单消息时发生异常: {}", order.getOrderId(), e);
            // 发生异常，拒绝消息，让其根据配置决定是否重回队列
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}