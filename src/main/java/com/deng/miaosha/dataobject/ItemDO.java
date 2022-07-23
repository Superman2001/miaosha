package com.deng.miaosha.dataobject;

import lombok.Data;

@Data
public class ItemDO {

    private Integer id;

    private String title;

    //todo 数据库中直接用decimal类型，这里也直接用BigDecimal表示
    private Double price;

    private String description;

    private String imgUrl;

}