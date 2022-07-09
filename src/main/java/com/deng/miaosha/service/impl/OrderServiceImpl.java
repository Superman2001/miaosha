package com.deng.miaosha.service.impl;

import com.deng.miaosha.dao.OrderDOMapper;
import com.deng.miaosha.dao.SequenceDOMapper;
import com.deng.miaosha.dataobject.OrderDO;
import com.deng.miaosha.dataobject.SequenceDO;
import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import com.deng.miaosha.service.ItemService;
import com.deng.miaosha.service.OrderService;
import com.deng.miaosha.service.UserService;
import com.deng.miaosha.service.model.ItemModel;
import com.deng.miaosha.service.model.OrderModel;
import com.deng.miaosha.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Validated
public class OrderServiceImpl implements OrderService {
    @Autowired
    private UserService userService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private OrderDOMapper orderDOMapper;
    @Autowired
    private SequenceDOMapper sequenceDOMapper;

    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount) throws BusinessException {
        //校验购买数量是否合法
        if(amount <= 0){  //若有限购数量或起购数量限制
            throw new BusinessException(EmBusinessError.BUY_AMOUNT_ERROR);
        }
        //校验用户是否合法
        UserModel userModel = userService.getUserById(userId);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
        //校验下单的商品是否存在
        ItemModel itemModel = itemService.getItemById(itemId);
        if(itemModel == null){
            throw new BusinessException(EmBusinessError.ITEM_NOT_EXIST);
        }
        //若传过来了活动id，校验活动信息
        if(promoId != null){
            //若该商品没有还未结束的秒杀活动，或有还未结束的秒杀活动但与promoId不对应
            if(itemModel.getPromoModel() == null || !promoId.equals(itemModel.getPromoModel().getId())){
                throw new BusinessException(EmBusinessError.PROMO_ERROR);
            }else if(itemModel.getPromoModel().getStatus() != 2){  //若活动不是正在进行
                throw new BusinessException(EmBusinessError.PROMO_NOT_START);
            }
        }

        //减库存（在下单时减库存，而不是支付减库存（可能会超卖））
        boolean flag = itemService.decreaseStock(itemId, amount);
        if(!flag){ //减库存失败，库存不够
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        //注意：先减库存后再把订单写入数据库，因为库存可能会不够，需要先执行减库存来看库存够不够
        //减库存失败事务不会回滚，减库存失败不是sql语句执行失败，而是sql语句执行成功后的结果从业务上看失败了

        //订单入库
        OrderModel orderModel = new OrderModel();
        //设置交易流水号(订单号)
        orderModel.setId(generateOrderNo());
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);

        if(promoId != null){
            orderModel.setPromoId(promoId);
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            orderModel.setItemPrice(itemModel.getPrice());
        }

        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));

        OrderDO orderDO = convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        //加销量
        //todo 可以设计为事务提交后使用异步的方式调用，因为销量大多数情况下仅用来给用户展示，且即时性要求不高，不影响业务
        itemService.increaseSales(itemId, amount);

        return orderModel;
    }


    /**
     * 生成订单号
     * @return 16位订单号
     */
//    @Transactional(propagation = Propagation.REQUIRES_NEW) //开启新事务，且在本方法执行完立即提交
    //@Transactional注解加在private方法上没有意义
    private String generateOrderNo(){
        //订单号有16位
        StringBuilder stringBuilder = new StringBuilder();
        //前8位为时间信息，年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        stringBuilder.append(nowDate);

        //中间6位为自增序列(假设一天内订单数不超过6位数)
        //获取当前sequence
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


    private OrderDO convertFromOrderModel(OrderModel orderModel){
        if(orderModel == null){
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel,orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDO;
    }
}
