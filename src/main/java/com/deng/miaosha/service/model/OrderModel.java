package com.deng.miaosha.service.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
/*
在本项目中，只考虑一个订单只买一种商品且只买一件的情况，下订单成功就代表购买商品成功而不考虑支付
 */
public class OrderModel {
    //订单号
    private String id;

    //购买的用户id
    private Integer userId;

    //购买的商品id
    private Integer itemId;

    //若非空，则表示是以秒杀商品方式下单
    private Integer promoId;

    //购买商品的单价,若promoId非空，则表示秒杀商品价格
    private BigDecimal itemPrice;

    //购买数量
    private Integer amount;

    //购买订单金额,等于 amount * itemPrice
    private BigDecimal orderPrice;
}
