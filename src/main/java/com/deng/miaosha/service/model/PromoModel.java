package com.deng.miaosha.service.model;

import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
//本项目中，只针对单品降价活动，且一种商品在同一时刻只有一个正在进行的秒杀活动
public class PromoModel {
    private Integer id;

//    //秒杀活动状态 1表示还未开始，2表示进行中，3表示已结束（是随时间动态变化的）
//    private Integer status;

    //秒杀活动名称
    private String promoName;

    //秒杀活动的适用商品
    private Integer itemId;

    //秒杀活动的商品价格
    private BigDecimal promoItemPrice;

    //活动库存
    private Integer promoItemStock;

    //秒杀活动的开始时间
    private DateTime startDate;

    //秒杀活动的结束时间
    private DateTime endDate;
}