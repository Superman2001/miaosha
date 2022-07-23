package com.deng.miaosha.dataobject;

import lombok.Data;

@Data
public class PromoStockDO {
    private Integer id;

    private Integer stock;

    private Integer promoId;

    private Integer itemId;
}
