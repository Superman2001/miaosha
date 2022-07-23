package com.deng.miaosha.service;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.service.model.PromoModel;

import java.util.List;

public interface PromoService {

    //根据活动id从数据库获取活动
    PromoModel getPromoById(Integer promoId);

    //根据活动id从redis获取活动
    PromoModel getPromoByIdFromRedis(Integer promoId);

    List<PromoModel> getPromoByItemId(Integer itemId);

    //发布活动
    void publishPromo(PromoModel promoModel);

    PromoModel findRecentPromoByItemId(Integer itemId);

    //判断活动状态（1表示还未开始，2表示进行中，3表示已结束）
    Integer judgePromoStatus(PromoModel promoModel);

    //扣减扣减（数据库中的）活动库存
    boolean decreasePromoStock(Integer promoId, Integer amount) throws BusinessException;

    //扣减（redis中的）活动库存
    boolean decreasePromoStockFromRedis(Integer itemId, Integer amount) throws BusinessException;

    //增加（redis中的）活动库存
    //（当创建订单发生异常时，回补redis中库存）
    void increaseStock(Integer promoId, Integer amount) throws BusinessException;
}
