package com.deng.miaosha.dataobject;

import lombok.Data;

import java.util.Date;

@Data
public class PromoDO {

    private Integer id;

    private String promoName;

    private Date startDate;

    private Date endDate;

    private Integer itemId;

    private Double promoItemPrice;

}