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
import org.springframework.data.redis.core.RedisTemplate;
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
    @Autowired
    private RedisTemplate redisTemplate;

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

    //从数据库中获取商品信息（不带活动）
    public ItemModel getItemById(Integer itemId){
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(itemId);
        //若商品不存在
        if(itemDO == null){
            return null;
        }
        //若存在，获取其库存
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemId);
        //将 itemDO + itemStockDO -> itemModel
        ItemModel itemModel = convertModelFromDataObject(itemDO, itemStockDO);

        return itemModel;
    }

    public ItemModel getItemByIdWithPromoFromRedis(Integer itemId){
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_"+itemId);
        return itemModel;
    }


    //根据活动id从数据库获取活动
    @Override
    public ItemModel getItemByIdWithPromo(Integer itemId) throws BusinessException {
        ItemModel itemModel = getItemById(itemId);


        //获取未开始或正在进行的秒杀活动
//        List<PromoModel> promoModelList = promoService.getPromoByItemId(id);
//        if(promoModelList.size() == 0){  //若没有对应的秒杀活动
//
//        }
//        if(promoModel != null && promoModel.getStatus() != 3){
//            itemModel.setPromoModel(promoModel);
//        }

        return itemModel;
    }

    //根据活动id从redis获取活动


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


    //扣减（数据库中的）商品库存
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





    //todo 删除销量字段，本项目不考虑普通销量和活动销量
//    @Override
//    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
//        itemDOMapper.increaseSales(itemId, amount);
//    }





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
