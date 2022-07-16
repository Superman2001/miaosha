package com.deng.miaosha.dao;

import com.deng.miaosha.dataobject.PromoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PromoDOMapper {

    int deleteByPrimaryKey(Integer id);

    int insert(PromoDO record);

    int insertSelective(PromoDO record);

    PromoDO selectByPrimaryKey(Integer id);

    List<PromoDO> selectByItemId(Integer itemId);

    int updateByPrimaryKeySelective(PromoDO record);

    int updateByPrimaryKey(PromoDO record);
}