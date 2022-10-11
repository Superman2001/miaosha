package com.deng.miaosha.dao;

import com.deng.miaosha.dataobject.PromoStockDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PromoStockDOMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PromoStockDO record);

    int insertSelective(PromoStockDO record);

    PromoStockDO selectByPrimaryKey(Integer id);

    PromoStockDO selectByPromoId(Integer promoId);

    //减库存
    int decreaseStock(@Param("promoId") Integer promoId, @Param("amount") Integer amount);

    //加库存
    int increaseStock(@Param("promoId") Integer promoId, @Param("amount") Integer amount);

    int updateByPrimaryKeySelective(PromoStockDO record);

    int updateByPrimaryKey(PromoStockDO record);
}
