package com.deng.miaosha.mq;

import com.alibaba.fastjson.JSON;
import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.mq.message.StockMessage;
import com.deng.miaosha.service.OrderService;
import com.deng.miaosha.service.model.OrderModel;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DecreaseStockProducer {
    @Value("${mq.topic.decreasePromoStock}")
    private String topicName;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Autowired
    private RedisTemplate<Object,Object> redisTemplate;

    @Autowired
    private CancelOrderProduer cancelOrderProduer;

    @Autowired
    private OrderService orderService;

    @PostConstruct
    public void init(){
    }


    //发送事务消息
    public boolean sendTransactionMsg(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId){
        //定义消息
        StockMessage stockMessage = new StockMessage();
        stockMessage.setPromoId(promoId);
        stockMessage.setItemId(itemId);
        stockMessage.setAmount(amount);
        stockMessage.setStockLogId(stockLogId);

        //定义其他信息（待创建的订单信息）
        Map<String,Object> orderInfo = new HashMap<>();
        orderInfo.put("userId",userId);
        orderInfo.put("itemId",itemId);
        orderInfo.put("promId",promoId);
        orderInfo.put("amount",amount);

        //发送半事务消息
        System.out.println("发送消息:"+stockMessage);//
        Message<StockMessage> message = MessageBuilder.withPayload(stockMessage).build();
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(topicName, message, orderInfo);
        if(result.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE){
            //事务消息提交，总事务执行成功
            return true;
        }
        //事务消息回滚，总事务执行失败
        return false;
    }


    //将自动注入rocketMQTemplate的transactionListener中
    @RocketMQTransactionListener(rocketMQTemplateBeanName = "rocketMQTemplate")
    class TransactionListenerImpl implements RocketMQLocalTransactionListener{
        //执行本地事务（在发送完事务消息后执行该方法）
        @Override
        public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object o) {
            //获取消息中的stockLogId
            String msg = new String((byte[]) message.getPayload());
            StockMessage stockMessage = JSON.parseObject(msg, StockMessage.class);
            String stockLogId = stockMessage.getStockLogId();

            //获取待创建的订单信息
            Map<String,Object> orderInfo = (Map<String, Object>) o;
            Integer userId = (Integer) orderInfo.get("userId");
            Integer itemId = (Integer) orderInfo.get("itemId");
            Integer promoId = (Integer) orderInfo.get("promId");
            Integer amount = (Integer) orderInfo.get("amount");

            try {
                //执行本地事务
                OrderModel order = orderService.createOrder(userId, itemId, promoId, amount);
                //发送取消订单的延时消息
                cancelOrderProduer.sendCancelOrderMsg(order.getId(), promoId, itemId, amount);
            } catch (BusinessException businessException) {  //本地事务失败
                //设置对应的库存消息的状态为回滚状态
                //todo 设置数据库
                redisTemplate.opsForValue().set("stock_msg_state_"+stockLogId, 2,10, TimeUnit.MINUTES);
                return RocketMQLocalTransactionState.ROLLBACK;
            }
            //本地事务成功，设置对应的库存消息的状态为提交状态
            //todo 设置数据库
            redisTemplate.opsForValue().set("stock_msg_state_"+stockLogId, 1,10, TimeUnit.MINUTES);
            return RocketMQLocalTransactionState.COMMIT;
        }

        //回调检查本地事务状态
        @Override
        public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
            //获取消息中的stockLogId
            String msg = new String((byte[]) message.getPayload());
            StockMessage stockMessage = JSON.parseObject(msg, StockMessage.class);
            String stockLogId = stockMessage.getStockLogId();
            //回查订单状态
            Integer state = (Integer) redisTemplate.opsForValue().get("stock_msg_state_"+stockLogId);
            if(state == null){  //若redis中订单状态已失效
                //todo 去数据库中查
            }
            if(state == 1){  //提交状态
                return RocketMQLocalTransactionState.COMMIT;
            }else if(state == 2){  //回滚状态
                return RocketMQLocalTransactionState.ROLLBACK;
            }else{  //未知状态
                return RocketMQLocalTransactionState.UNKNOWN;
            }
        }
    }

}
