package com.deng.miaosha.service;

import com.deng.miaosha.service.model.PromoModel;

import java.util.List;

public interface PromoService {

    List<PromoModel> getPromoByItemId(Integer itemId);

    //发布活动
    void publishPromo(PromoModel promoModel);

//    Integer judgePromoStatus(PromoModel promoModel);

    PromoModel findRecentPromoByItemId(Integer itemId);
}
