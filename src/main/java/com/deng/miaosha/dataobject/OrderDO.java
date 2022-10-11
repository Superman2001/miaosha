package com.deng.miaosha.dataobject;

import lombok.Data;

import java.util.Date;

@Data
public class OrderDO {
    private String id;

    private Integer userId;

    private Integer itemId;

    private Double itemPrice;

    private Integer amount;

    private Double orderPrice;

    private Integer promoId;

    private Date createTime;

    private Integer state;
}