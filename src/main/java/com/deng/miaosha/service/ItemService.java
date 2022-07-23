package com.deng.miaosha.service;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.service.model.ItemModel;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public interface ItemService {
    //创建商品
    ItemModel createItem(@NotNull @Valid ItemModel itemModel) throws BusinessException;

    //从数据库获取商品
    ItemModel getItemById(Integer id) throws BusinessException;

    //获取商品（带活动）
    ItemModel getItemByIdWithPromo(Integer id) throws BusinessException;

    //将商品信息存入redis中（带活动）（使用分布式锁防止缓存击穿） //todo
    ItemModel cacheItemToRedis(Integer itemId) throws BusinessException;

    //从缓存中获取商品信息（带活动）
    ItemModel getItemByIdFromCache(Integer itemId) throws BusinessException;

    //刷新商品缓存（刷新本地缓存和redis中缓存）
    ItemModel refreshItemCache(Integer itemId) throws BusinessException;

    //商品列表浏览
    List<ItemModel> listItem();

    //减库存
    boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException;

}
