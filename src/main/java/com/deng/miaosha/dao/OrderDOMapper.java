package com.deng.miaosha.dao;

import com.deng.miaosha.dataobject.OrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OrderDOMapper {
    int deleteByPrimaryKey(String id);

    int insert(OrderDO record);

    int insertSelective(OrderDO record);

    OrderDO selectByPrimaryKey(String id);

    //根据用户id查订单
    List<OrderDO> selectByUserId(Integer userId);

    int updateByPrimaryKeySelective(OrderDO record);

    int updateByPrimaryKey(OrderDO record);

    int updateState(OrderDO record);

    int payOrder(String id);

    int cancelOrder(String id);
}