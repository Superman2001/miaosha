package com.deng.miaosha.service.impl;

import com.deng.miaosha.dao.ItemDOMapper;
import com.deng.miaosha.dao.ItemStockDOMapper;
import com.deng.miaosha.dataobject.ItemDO;
import com.deng.miaosha.dataobject.ItemStockDO;
import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import com.deng.miaosha.service.ItemService;
import com.deng.miaosha.service.PromoService;
import com.deng.miaosha.service.model.ItemModel;
import com.deng.miaosha.service.model.PromoModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ItemDOMapper itemDOMapper;
    @Autowired
    private ItemStockDOMapper itemStockDOMapper;
    @Autowired
    private PromoService promoService;


    @Override
    @Transactional
    public ItemModel createItem(@NotNull @Valid ItemModel itemModel) throws BusinessException {
        //将itemModel -> ItemDO
        ItemDO itemDO = convertItemDOFromItemModel(itemModel);
        itemDOMapper.insertSelective(itemDO);

        //将自增主键值赋给 itemModel
        itemModel.setId(itemDO.getId());

        //将itemModel -> ItemStockDO
        ItemStockDO itemStockDO = convertItemStockDOFromItemModel(itemModel);
        itemStockDOMapper.insertSelective(itemStockDO);

        //返回创建完成的对象
        return getItemById(itemModel.getId());
    }


    @Override
    public ItemModel getItemById(Integer id) {
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        // 商品不存在
        if(itemDO == null){
            return null;  //todo 是否应该抛出异常
        }
        //若存在，获取其库存
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(id);

        //将 itemDO + itemStockDO -> itemModel
        ItemModel itemModel = convertModelFromDataObject(itemDO, itemStockDO);

        //获取未开始或正在进行的秒杀活动
        PromoModel promoModel = promoService.getPromoByItemId(id);
        if(promoModel != null && promoModel.getStatus() != 3){
            itemModel.setPromoModel(promoModel);
        }

        return itemModel;
    }


    @Override
    public List<ItemModel> listItem() {
        //获取所有商品
        List<ItemDO> itemDOList = itemDOMapper.listItem();
        //获取每个商品的库存并合并为 ItemModel
        List<ItemModel> itemModelList =  itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            return convertModelFromDataObject(itemDO,itemStockDO);
        }).collect(Collectors.toList());
        return itemModelList;
    }


    @Override
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);
        if(affectedRow > 0){
            //更新库存成功
            return true;
        }else{
            //更新库存失败（库存不够）
            return false;
        }
    }


    @Override
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemDOMapper.increaseSales(itemId, amount);
    }





    private ItemDO convertItemDOFromItemModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel,itemDO);
        //将price由 BigDecimal转为 Double
        itemDO.setPrice(itemModel.getPrice().doubleValue());
        return itemDO;
    }

    private ItemStockDO convertItemStockDOFromItemModel(ItemModel itemModel){
        if(itemModel == null){
            return null;
        }
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());
        return itemStockDO;
    }

    private ItemModel convertModelFromDataObject(ItemDO itemDO,ItemStockDO itemStockDO){
        if(itemDO == null){
            return null;
        }
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO,itemModel);
        //将price由 Double转为 BigDecimal
        itemModel.setPrice(BigDecimal.valueOf(itemDO.getPrice()));  //todo BigDecimal和Double的相互转换

        if(itemStockDO != null && itemStockDO.getItemId().equals(itemDO.getId())){
            itemModel.setStock(itemStockDO.getStock());
        }
        return itemModel;
    }
}
