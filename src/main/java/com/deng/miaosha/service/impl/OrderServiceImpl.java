package com.deng.miaosha.service.impl;

import com.deng.miaosha.dao.OrderDOMapper;
import com.deng.miaosha.dao.SequenceDOMapper;
import com.deng.miaosha.dataobject.OrderDO;
import com.deng.miaosha.dataobject.SequenceDO;
import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import com.deng.miaosha.mq.CancelOrderProduer;
import com.deng.miaosha.service.ItemService;
import com.deng.miaosha.service.OrderService;
import com.deng.miaosha.service.PromoService;
import com.deng.miaosha.service.UserService;
import com.deng.miaosha.service.model.ItemModel;
import com.deng.miaosha.service.model.OrderModel;
import com.deng.miaosha.service.model.PromoModel;
import com.deng.miaosha.service.model.UserModel;
import com.deng.miaosha.utils.TimeUtils;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Validated
public class OrderServiceImpl implements OrderService {
    @Autowired
    private UserService userService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private PromoService promoService;
    @Autowired
    private OrderDOMapper orderDOMapper;
    @Autowired
    private SequenceDOMapper sequenceDOMapper;
    @Autowired
    private RedisTemplate<Object,Object> redisTemplate;
    @Autowired
    private CancelOrderProduer cancelOrderProduer;


    //获取下单资格（普通下单）
    @Override
    public boolean getOrderQualification(Integer userId, Integer itemId, Integer amount) throws BusinessException{
        //校验商品
        ItemModel itemModel = itemService.getItemByIdFromCache(itemId);
        if(itemModel == null){  //商品不存在
            throw new BusinessException(EmBusinessError.ITEM_NOT_EXIST);
        }

        //校验用户
        UserModel userModel = userService.getUserByIdFromRedis(userId);
        if(userModel == null){  //用户信息失效
            throw new BusinessException(EmBusinessError.USER_LOGIN_TIMEOUT);
        }

        //每次起购数量和每个用户限购数量
        if(amount <= 0){
            throw new BusinessException(EmBusinessError.BUY_AMOUNT_ERROR);
        }

        return true;
    }


    //获取下单资格（活动下单）
    //return true表示有资格下单
    //其他情况抛出异常
    @Override
    public boolean getOrderQualification(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException{
        //校验商品
        ItemModel itemModel = itemService.getItemByIdFromCache(itemId);
        if(itemModel == null){  //商品不存在
            throw new BusinessException(EmBusinessError.ITEM_NOT_EXIST);
        }

        //校验活动
        //PromoModel promoModel = promoService.getPromoByIdFromRedis(promoId);
        PromoModel promoModel = itemModel.getPromoModel();
        if(promoModel == null || !promoModel.getId().equals(promoId)){  //商品和活动不对应
            throw new BusinessException(EmBusinessError.PROMO_ERROR);
        }
        if(promoService.judgePromoStatus(promoModel) != 2){  //活动不存在或活动不是正在进行
            throw new BusinessException(EmBusinessError.PROMO_ERROR);
        }
        if(redisTemplate.hasKey("promo_item_stock_sell_out"+promoId)){  //判断是否已售完
            throw new BusinessException(EmBusinessError.STOCK_SELL_OUT);
        }

        //校验用户
        UserModel userModel = userService.getUserByIdFromRedis(userId);
        if(userModel == null){  //用户信息失效
            throw new BusinessException(EmBusinessError.USER_LOGIN_TIMEOUT);
        }

        //每次起购数量和每个用户限购数量
        if(amount <= 0){
            throw new BusinessException(EmBusinessError.BUY_AMOUNT_ERROR);
        }

        //用户请求频率限制标记，每2s只能请求一次
        redisTemplate.opsForValue().set("limit_request_order_user_"+userId+"_promo_"+promoId,true,2,TimeUnit.SECONDS);

        //减秒杀大闸数量
        long doorCount = redisTemplate.opsForValue().increment("promo_door_count_"+promoId,-1);
        if(doorCount < 0){
            throw new BusinessException(EmBusinessError.PROMO_BUSY);
        }

        return true;
    }


    //创建订单（普通下单）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderModel createOrder(Integer userId, Integer itemId, Integer amount) throws BusinessException{
        //从缓存中获取商品
        ItemModel itemModel = itemService.getItemByIdFromCache(itemId);

        boolean flag = itemService.decreaseStock(itemId, amount);
        if(!flag){ //减库存失败，库存不够
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }
        //订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setId(generateOrderNo());
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        orderModel.setItemPrice(itemModel.getPrice());
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
        orderModel.setCreateTime(DateTime.now());
        orderModel.setState(0);
        //转换orderModel -> orderDO
        OrderDO orderDO = convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        //发送取消订单的延时消息
        cancelOrderProduer.sendCancelOrderMsg(orderModel.getId(), orderModel.getPromoId(), orderDO.getItemId(), orderModel.getAmount());

        return orderModel;
    }


    //创建订单（活动下单）
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException {
        //从缓存中获取商品
        ItemModel itemModel = itemService.getItemByIdFromCache(itemId);

        //减库存（在下单时减库存，而不是支付减库存（可能会超卖））
        boolean flag = promoService.decreasePromoStockFromRedis(promoId, amount);
        if(!flag){ //减库存失败，库存不够
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        try{
            //订单入库
            OrderModel orderModel = new OrderModel();
            orderModel.setId(generateOrderNo());
            orderModel.setUserId(userId);
            orderModel.setItemId(itemId);
            orderModel.setAmount(amount);
            orderModel.setPromoId(promoId);
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
            orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));
            orderModel.setCreateTime(DateTime.now());
            orderModel.setState(0);
            //转换orderModel -> orderDO
            OrderDO orderDO = convertFromOrderModel(orderModel);
            orderDOMapper.insertSelective(orderDO);

            return orderModel;
        }catch (Exception e){  //在减redis库存之后若遇到异常，需要回补redis中库存
            promoService.increaseStockFromRedis(promoId, amount);
            throw e;
        }

    }



    //生成订单号（16位订单号）
//    @Transactional(propagation = Propagation.REQUIRES_NEW) //开启新事务，且在本方法执行完立即提交
    //@Transactional注解加在private方法上没有意义
    @Override
    public String generateOrderNo(){
        //订单号有16位
        StringBuilder stringBuilder = new StringBuilder();
        //前8位为时间信息，年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        stringBuilder.append(nowDate);

        //中间6位为自增序列(假设一天内订单数不超过6位数)
        //获取当前sequence  //todo 放到redis中
        int sequence = 0;
        SequenceDO sequenceDO =  sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        //更新CurrentValue
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        //将当前sequence转为6位字符串（在前面补0）
        String sequenceStr = String.valueOf(sequence);
        for(int i = 0; i < 6-sequenceStr.length();i++){
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        //最后2位为分库分表位,暂时写死（可以设计根据用户id分库分表，使同一用户的订单在同一个库里）
        stringBuilder.append("00");

        return stringBuilder.toString();
    }


    //根据用户id查订单
    public List<OrderModel> getOrderByUser(Integer userId){
        List<OrderDO> orderDOList = orderDOMapper.selectByUserId(userId);
        List<OrderModel> orderModelList = new ArrayList<>();
        for(OrderDO orderDO : orderDOList){
            orderModelList.add(convertOrderDOToOrderModel(orderDO));
        }
        return orderModelList;
    }


    //支付（改变订单状态为 1）
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(String orderId) throws BusinessException {
        int flag = orderDOMapper.payOrder(orderId);
        if(flag == 0){  //支付失败
            OrderDO orderDO = orderDOMapper.selectByPrimaryKey(orderId);
            if(orderDO == null){
                throw new BusinessException(EmBusinessError.ORDER_NOT_EXIST);
            }else{
                int state = orderDO.getState();
                if(state == 1){  //已支付
                    throw new BusinessException(EmBusinessError.PAYED_ERROR);
                }else if(state == 2){  //已取消
                    throw new BusinessException(EmBusinessError.CANCELED_ERROR);
                }
            }
        }
        return true;
    }

    //取消（改变订单状态为 2 并回补库存）
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String orderId) throws BusinessException {
        //检查订单状态，若为未支付，则改变订单状态
        int flag = orderDOMapper.cancelOrder(orderId);
        if(flag == 0){  //取消失败
            OrderDO orderDO = orderDOMapper.selectByPrimaryKey(orderId);
            if(orderDO == null){
                throw new BusinessException(EmBusinessError.ORDER_NOT_EXIST);
            }else{
                int state = orderDO.getState();
                if(state == 1){  //已支付
                    throw new BusinessException(EmBusinessError.PAYED_ERROR);
                }else if(state == 2){  //已取消
                    throw new BusinessException(EmBusinessError.CANCELED_ERROR);
                }
            }
        }
        //回补库存
        OrderDO orderDO = orderDOMapper.selectByPrimaryKey(orderId);
        promoService.increaseStockFromRedis(orderDO.getPromoId(),orderDO.getAmount());
        promoService.increasePromoStock(orderDO.getPromoId(),orderDO.getAmount());
        return true;
    }



    private OrderDO convertFromOrderModel(OrderModel orderModel){
        if(orderModel == null){
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel,orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        orderDO.setCreateTime(TimeUtils.convertDateTimeToDate(orderModel.getCreateTime()));
        return orderDO;
    }

    private OrderModel convertOrderDOToOrderModel(OrderDO orderDO){
        if(orderDO == null){
            return null;
        }
        OrderModel orderModel = new OrderModel();
        BeanUtils.copyProperties(orderDO ,orderModel);
        orderModel.setItemPrice(BigDecimal.valueOf(orderDO.getItemPrice()));
        orderModel.setOrderPrice(BigDecimal.valueOf(orderDO.getOrderPrice()));
        orderModel.setCreateTime(TimeUtils.convertDateToDateTime(orderDO.getCreateTime()));
        return orderModel;
    }

}
