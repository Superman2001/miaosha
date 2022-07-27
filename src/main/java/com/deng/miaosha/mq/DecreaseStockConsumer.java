package com.deng.miaosha.mq;

import com.deng.miaosha.mq.message.StockMessage;
import com.deng.miaosha.service.PromoService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RocketMQMessageListener(topic = "${mq.topic.decreasePromoStock}", consumerGroup = "${mq.group.consumer.decreasePromoStock}")
public class DecreaseStockConsumer implements RocketMQListener<StockMessage> {
    @Autowired
    private PromoService promoService;
    @Autowired
    private RedisTemplate<Object,Object> redisTemplate;

    @Override
    public void onMessage(StockMessage msg) {
        System.out.println("收到消息:"+msg);//
        promoService.decreasePromoStock(msg.getPromoId(), msg.getAmount());
        redisTemplate.opsForValue().set("stock_msg_state_"+msg.getStockLogId(), 3,10, TimeUnit.MINUTES);
    }
}
