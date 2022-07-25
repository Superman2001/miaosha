package com.deng.miaosha.service;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.service.model.OrderModel;

import javax.validation.constraints.Min;

public interface OrderService {
    //获取下单资格（活动下单）
    //return true表示有资格下单
    boolean getOrderQualification(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException;

    OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String orderId) throws BusinessException;

    //生成订单号（16位订单号）
//    @Transactional(propagation = Propagation.REQUIRES_NEW) //开启新事务，且在本方法执行完立即提交
    //@Transactional注解加在private方法上没有意义
    String generateOrderNo();
}
