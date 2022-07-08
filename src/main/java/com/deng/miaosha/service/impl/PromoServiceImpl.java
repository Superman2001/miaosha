package com.deng.miaosha.service.impl;

import com.deng.miaosha.dao.PromoDOMapper;
import com.deng.miaosha.dataobject.PromoDO;
import com.deng.miaosha.service.PromoService;
import com.deng.miaosha.service.model.PromoModel;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PromoServiceImpl implements PromoService {
    @Autowired
    private PromoDOMapper promoDOMapper;

    /**
     * 获取对应商品的 即将开始 或 正在进行 的秒杀活动
     * @param itemId
     * @return
     */
    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的秒杀活动
        //todo 实际情况一种商品的秒杀活动可能不止一个，应该获取所有秒杀活动再判断是否是已经结束还是即将开始或正在进行
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);
        if(promoDO == null){  //若没有秒杀活动，直接 return null
            return null;
        }

        //将 promoDO -> promoModel
        PromoModel promoModel = convertFromDataObject(promoDO);

        //判断当前时间秒杀活动是否即将开始或正在进行
        if(promoModel.getStartDate().isAfterNow()){  //未开始
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){  //已结束
            promoModel.setStatus(3);
        }else{  //正在进行
            promoModel.setStatus(2);
        }

        return promoModel;
    }



    private PromoModel convertFromDataObject(PromoDO promoDO){
        if(promoDO == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        promoModel.setPromoItemPrice(BigDecimal.valueOf(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}
