package com.deng.miaosha.service.impl;

import com.deng.miaosha.dao.PromoDOMapper;
import com.deng.miaosha.dao.PromoStockDOMapper;
import com.deng.miaosha.dataobject.PromoDO;
import com.deng.miaosha.dataobject.PromoStockDO;
import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import com.deng.miaosha.service.PromoService;
import com.deng.miaosha.service.model.PromoModel;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

//本项目中，仅针对单品降价活动，即一个活动只对应一个商品
@Service
public class PromoServiceImpl implements PromoService {
    @Autowired
    private PromoDOMapper promoDOMapper;
    @Autowired
    private PromoStockDOMapper promoStockDOMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    //扣减库存的lua脚本
    //扣减成功return 剩余库存数
    //扣减失败：库存不足return -1，其他原因return -2
    private static final String DECREASE_STOCK_LUA =
            "if(redis.call('exists', KEYS[1]) == 1) then"+
            "    local stock = tonumber(redis.call('get', KEYS[1]));"+
            "    local num = tonumber(ARGV[1]);"+
            "    if(stock >= num) then"+
            "        return redis.call('incrby', KEYS[1], 0 - num);"+
            "    else return -1;"+
            "    end;"+
            "else return -2;"+
            "end;";


    //根据活动id从数据库获取活动
    @Override
    public PromoModel getPromoById(Integer promoId){
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        PromoModel promoModel = convertFromDataObject(promoDO);
        if(promoModel != null){  //获取活动库存
            PromoStockDO promoStockDO = promoStockDOMapper.selectByPromoId(promoId);
            promoModel.setPromoItemStock(promoStockDO.getStock());
        }

        return promoModel;
    }


    //根据活动id从redis获取活动
    @Override
    public PromoModel getPromoByIdFromRedis(Integer promoId){
        PromoModel promoModel = (PromoModel) redisTemplate.opsForValue().get("promo_"+promoId);

//        if (promoModel != null) {
//            promoModel.setStatus(judgePromoStatus(promoModel));
//        }

        return promoModel;
    }


    //获取对应商品的所有秒杀活动
    @Override
    public List<PromoModel> getPromoByItemId(Integer itemId) {
        //获取商品对应的所有秒杀活动
        List<PromoDO> promoDOList = promoDOMapper.selectByItemId(itemId);

        //转换为 promoModelList
        List<PromoModel> promoModelList = promoDOList.stream().map(promoDO -> {
            PromoModel promoModel = convertFromDataObject(promoDO);
//            promoModel.setStatus(judgePromoStatus(promoModel));
            return promoModel;
        }).collect(Collectors.toList());

        return promoModelList;
    }


    //找到 正在进行 或 未开始但距离开始时间最近 的秒杀活动
    @Override
    public PromoModel findRecentPromoByItemId(Integer itemId){
        List<PromoModel> promoModelList = getPromoByItemId(itemId);
        if(promoModelList.size() == 0){  //若没有对应的秒杀活动
            return null;
        }

        List<PromoModel> readyPromoList = new ArrayList<>();
        for(PromoModel promoModel : promoModelList){
            Integer status = judgePromoStatus(promoModel);
            if(status == 2){  //若存在正在进行的活动
                return promoModel;
            }else if(status == 1){
                readyPromoList.add(promoModel);
            }
        }

        //根据开始时间对未开始的活动排序
        PromoModel recentPromo = readyPromoList.stream().min((promo1, promo2) -> {
            DateTime start1 = promo1.getStartDate();
            DateTime start2 = promo2.getStartDate();
            if(start1.isBefore(start2)){
                return -1;
            }else if(start1.isAfter(start2)){
                return 1;
            }else{
                return 0;
            }
        }).get();

        return recentPromo;
    }


    //发布活动，从商品库存中划分出指定的库存数量到活动库存中
    @Override
    @Transactional
    public void publishPromo(PromoModel promoModel) {
        //限制一种商品在同一时刻只有一个正在进行的秒杀活动

        //从商品库存中减去活动库存的数量

        //将活动写入数据库

        //因为是热点数据，不设置缓存失效时间，避免缓存击穿
        //将活动信息缓存到redis中
        redisTemplate.opsForValue().set("promo_"+promoModel.getId(), promoModel);
        //将活动库存缓存到redis中
        redisTemplate.opsForValue().set("promo_item_stock_"+promoModel.getId(), promoModel.getPromoItemStock());
        //将大闸的限制数字设到redis内
        redisTemplate.opsForValue().set("promo_door_count_"+promoModel.getId(), promoModel.getPromoItemStock() * 5);

        //刷新商品缓存
    }


    //todo 结束活动，将活动库存中剩余的返还到商品库存中,同时删除redis中键和值


    //判断活动状态（1表示还未开始，2表示进行中，3表示已结束）
    @Override
    public Integer judgePromoStatus(PromoModel promoModel){
        if(promoModel.getStartDate() == null || promoModel.getEndDate() == null){  //无法判断
            return null;
        }
        //判断当前时间秒杀活动是否即将开始或正在进行
        if(promoModel.getStartDate().isAfterNow()){  //未开始
            return 1;
        }else if(promoModel.getEndDate().isBeforeNow()){  //已结束
            return 3;
        }else{  //正在进行
            return 2;
        }
    }


    //扣减扣减（数据库中的）活动库存
    @Override
    @Transactional
    public boolean decreasePromoStock(Integer promoId, Integer amount) throws BusinessException {
        int affectedRow = promoStockDOMapper.decreaseStock(promoId,amount);
        if(affectedRow > 0){
            //更新库存成功
            return true;
        }else{
            //更新库存失败（库存不够）
            return false;
        }
    }


    //扣减（redis中的）活动库存
    @Override
    @Transactional
    public boolean decreasePromoStockFromRedis(Integer promoId, Integer amount) throws BusinessException {
        //lua脚本里的KEYS参数
        List<String> keys = new ArrayList<>();
        keys.add("promo_item_stock_"+promoId);
        // lua脚本里的ARGV参数
        List<String> args = new ArrayList<>();
        args.add(Integer.toString(amount));

        Long result = (Long) redisTemplate.execute((RedisCallback<Long>) redisConnection -> {
            Jedis jedis = (Jedis) redisConnection.getNativeConnection(); //单机版redis
            return (Long) jedis.eval(DECREASE_STOCK_LUA, keys, args);
        });

        if(result == null || result == -2){  //未知异常
            throw new BusinessException(EmBusinessError.STOCK_UNINITIALIZED);
        }else if(result == -1){  //库存不足
            return false;
        }else return true;
    }


    //增加（redis中的）活动库存
    //（当创建订单发生异常时，回补redis中库存）
    @Override
    public void increaseStock (Integer promoId, Integer amount) throws BusinessException{
        redisTemplate.opsForValue().increment("promo_item_stock_"+promoId, amount);
    }





    private PromoModel convertFromDataObject(PromoDO promoDO){
        if(promoDO == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        promoModel.setPromoItemPrice(BigDecimal.valueOf(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}
