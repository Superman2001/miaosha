package com.deng.miaosha.mq.message;

import lombok.Data;

//用于"超时未支付取消订单"的消息
@Data
public class CancelOrderMessage {

    private String orderId;

    private Integer promoId;

    private Integer itemId;

    private Integer amount;
}
