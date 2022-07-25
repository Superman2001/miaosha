package com.deng.miaosha.mq.message;

import lombok.Data;

import java.io.Serializable;

//"扣减数据库中库存"消息
@Data
public class DecreaseStockMsg implements Serializable {
    private Integer promoId;

    private Integer itemId;

    private Integer amount;

    private String orderId;
}
