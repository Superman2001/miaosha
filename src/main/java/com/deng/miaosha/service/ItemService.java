package com.deng.miaosha.service;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.service.model.ItemModel;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public interface ItemService {
    //创建商品
    ItemModel createItem(@NotNull @Valid ItemModel itemModel) throws BusinessException;

    //商品列表浏览
    List<ItemModel> listItem();

    //商品详情浏览
    ItemModel getItemById(Integer id);

    //减库存
    boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException;

    //加销量
    void increaseSales(Integer itemId,Integer amount) throws BusinessException;

}
