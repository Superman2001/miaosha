package com.deng.miaosha.mq;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.mq.message.CancelOrderMessage;
import com.deng.miaosha.service.OrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RocketMQMessageListener(topic = "${mq.topic.cancelOrder}", consumerGroup = "${mq.group.consumer.cancelOrder}",maxReconsumeTimes = 20)
public class CancelOrderConsumer implements RocketMQListener<CancelOrderMessage> {
    @Autowired
    private OrderService orderService;

    @Override
    public void onMessage(CancelOrderMessage msg) {
        System.out.println("收到延时消息:"+msg);//

        try {
            orderService.cancelOrder(msg.getOrderId());
        } catch (BusinessException businessException) {
            businessException.printStackTrace();
        }

    }
}
