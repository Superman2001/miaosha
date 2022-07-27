package com.deng.miaosha.mq.message;

import lombok.Data;

import java.io.Serializable;

//用于"扣减数据库中库存"的消息
@Data
public class StockMessage {
    private Integer promoId;

    private Integer itemId;

    private Integer amount;

    private String stockLogId;
}
