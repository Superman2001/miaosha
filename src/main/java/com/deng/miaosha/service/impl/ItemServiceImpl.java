package com.deng.miaosha.service.impl;

import com.deng.miaosha.cache.LocalCache;
import com.deng.miaosha.dao.ItemDOMapper;
import com.deng.miaosha.dao.ItemStockDOMapper;
import com.deng.miaosha.dataobject.ItemDO;
import com.deng.miaosha.dataobject.ItemStockDO;
import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.service.ItemService;
import com.deng.miaosha.service.PromoService;
import com.deng.miaosha.service.model.ItemModel;
import com.deng.miaosha.service.model.PromoModel;
import com.deng.miaosha.utils.LockUtils;
import com.deng.miaosha.utils.TimeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    @Autowired
    private LocalCache localCache;
    @Autowired
    private TimeUtils timeUtils;
    @Autowired
    private LockUtils lockUtils;

    //创建商品
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
    @Override
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


    //从数据库获取商品（带活动）
    @Override
    public ItemModel getItemByIdWithPromo(Integer itemId) throws BusinessException {
        ItemModel itemModel = getItemById(itemId);
        PromoModel promoModel = promoService.findRecentPromoByItemId(itemId);
        itemModel.setPromoModel(promoModel);

        return itemModel;
    }


    //将商品信息存入redis中（带活动）
    @Override
    public ItemModel cacheItemToRedis(Integer itemId) throws BusinessException {

        ItemModel itemModel = getItemByIdWithPromo(itemId);

        redisTemplate.opsForValue().set("item_"+itemId, itemModel);
        //设置过期时间
        int seconds = 600;
        if(itemModel.getPromoModel() != null){ //若有活动，在活动结束时过期（因为活动商品是热点数据）
            seconds = timeUtils.secondsBetweenNow(itemModel.getPromoModel().getEndDate());
        }
        redisTemplate.expire("item_"+itemId, seconds, TimeUnit.SECONDS);

        return itemModel;
    }


    //从缓存中获取商品信息（带活动）
    @Override
    public ItemModel getItemByIdFromCache(Integer itemId) throws BusinessException {
        ItemModel itemModel = null;

        //先从本地缓存中取商品信息
        itemModel = (ItemModel) localCache.getFromCommonCache("item_"+itemId);
        if(itemModel != null){  //若本地缓存中商品活动已结束，刷新本地缓存
            if(itemModel.getPromoModel() != null
                    && promoService.judgePromoStatus(itemModel.getPromoModel()) == 3){
                itemModel = null;
            }
        }
        if(itemModel == null){ //若在本地缓存中不存在
            //到redis中获取
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_"+itemId);
            if(itemModel == null){ //若在redis中不存在（使用分布式锁防止缓存击穿）
                //加分布式锁
                String lockValue = lockUtils.tryLock("item_lock_" + itemId);
                try{
                    itemModel = (ItemModel) redisTemplate.opsForValue().get("item_"+itemId);
                    if(itemModel == null){
                        //到数据库中获取并放入redis中
                        itemModel = cacheItemToRedis(itemId);
                    }
                }finally {
                    //解锁
                    lockUtils.unlock("item_lock_" + itemId, lockValue);
                }
            }
            //放入本地缓存中
            localCache.setCommonCache("item_"+itemId, itemModel);
        }

        return itemModel;
    }


    //刷新商品缓存（当活动发布、修改和商品修改时，刷新本地和redis中的商品缓存）
    @Override
    public ItemModel refreshItemCache(Integer itemId) throws BusinessException{
        //重新从数据库中查出并存入redis
        ItemModel itemModel = cacheItemToRedis(itemId);
        //同时重新存入本地缓存 //todo 利用mq发布-订阅更新所有应用服务器上的本地缓存
        localCache.setCommonCache("item_"+itemId, itemModel);
        return itemModel;
    }


    //获取全部商品（不带活动）
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
    @Transactional
    public boolean decreaseStock(Integer itemId, Integer amount){
        int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);
        if(affectedRow > 0){
            //更新库存成功
            return true;
        }else{
            //更新库存失败（库存不够）
            return false;
        }
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
