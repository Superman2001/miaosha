package com.deng.miaosha.controller.viewobject;

import lombok.Data;
import org.joda.time.DateTime;

import java.math.BigDecimal;

@Data
public class OrderVO {
    //订单号
    private String id;

//    //购买的商品id
//    private Integer itemId;

    //购买的商品名
    private String itemName;

    //购买商品的单价,若promoId非空，则表示秒杀商品价格
    private BigDecimal itemPrice;

    //购买数量
    private Integer amount;

    //购买订单金额,等于 amount * itemPrice
    private BigDecimal orderPrice;

    //购买的时间
    private String createTime;

    //订单的状态
    private String state;
}
