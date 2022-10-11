package com.deng.miaosha.mq;

import com.alibaba.fastjson.TypeReference;
import com.deng.miaosha.mq.message.CancelOrderMessage;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class CancelOrderProduer {
    @Value("${mq.topic.cancelOrder}")
    private String topicName;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;


    @PostConstruct
    public void init(){
    }

    public void sendCancelOrderMsg(String orderId, Integer promoId, Integer itemId, Integer amount){
        //定义消息
        CancelOrderMessage cancelOrderMessage = new CancelOrderMessage();
        cancelOrderMessage.setOrderId(orderId);
        cancelOrderMessage.setPromoId(promoId);
        cancelOrderMessage.setItemId(itemId);
        cancelOrderMessage.setAmount(amount);

        Message<CancelOrderMessage> message = MessageBuilder.withPayload(cancelOrderMessage).build();
        //同步发送延时消息，30min 16
        SendResult sendResult = rocketMQTemplate.syncSend(topicName, message, 5000, 6);

        //发送延时消息
        System.out.println("发送延时消息:"+ message);
    }
}
