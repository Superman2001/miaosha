package com.deng.miaosha.service;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.service.model.OrderModel;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.Min;

public interface OrderService {
    //获取下单资格（普通下单）
    boolean getOrderQualification(Integer userId, Integer itemId, Integer amount) throws BusinessException;

    //获取下单资格（活动下单）
    boolean getOrderQualification(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException;

    //创建订单（普通下单）
    OrderModel createOrder(Integer userId, Integer itemId, Integer amount) throws BusinessException;

    //创建订单（活动下单）
    OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException;

    //生成订单号（16位订单号）
//    @Transactional(propagation = Propagation.REQUIRES_NEW) //开启新事务，且在本方法执行完立即提交
    //@Transactional注解加在private方法上没有意义
    String generateOrderNo();
}
