package com.deng.miaosha.controller;

import com.deng.miaosha.controller.viewobject.ItemVO;
import com.deng.miaosha.controller.viewobject.OrderVO;
import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import com.deng.miaosha.mq.DecreaseStockProducer;
import com.deng.miaosha.response.CommonReturnType;
import com.deng.miaosha.service.ItemService;
import com.deng.miaosha.service.OrderService;
import com.deng.miaosha.service.model.OrderModel;
import com.deng.miaosha.service.model.UserModel;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate<Object,Object> redisTemplate;
    @Autowired
    private DecreaseStockProducer producer;

    //令牌桶
    private RateLimiter orderCreateRateLimiter;
    //线程池
    private ExecutorService executorService;


    @PostConstruct
    public void init(){
        //设置令牌桶每秒发放的令牌数
        orderCreateRateLimiter = RateLimiter.create(500);
        //设置线程池中的线程数
        executorService = Executors.newFixedThreadPool(20);
    }


    //创建订单
    //通过前端url上传过来秒杀活动id，然后下单接口内校验对应id是否属于对应商品且活动已开始
    @PostMapping("/createorder")
    public CommonReturnType createOrder(@RequestParam(name = "itemId")Integer itemId,
                                        @RequestParam(name = "promoId",required = false)Integer promoId,
                                        @RequestParam(name = "amount")Integer amount,
                                        @RequestParam(name = "token")String token) throws BusinessException {
        //尝试获取令牌桶中令牌
        if(!orderCreateRateLimiter.tryAcquire()){
            throw new BusinessException(EmBusinessError.PROMO_BUSY);
        }

        if(StringUtils.isEmpty(token)){  //用户未登录
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //从 redis中获取到登录的用户id
        Integer userId = (Integer) redisTemplate.opsForValue().get("token_"+token);
        if(userId == null){  //用户登录信息过期
            throw new BusinessException(EmBusinessError.USER_LOGIN_TIMEOUT);
        }

        //判断用户请求频率是否被限制
        if(redisTemplate.hasKey("limit_request_order_user_"+userId+"_promo_"+promoId)){
            throw new BusinessException(EmBusinessError.PROMO_BUSY);
        }

        boolean qualification = false;
        if(promoId == null){ //普通下单
            //先获取下单资格，若没有资格会抛出对应原因的异常
            qualification = orderService.getOrderQualification(userId, itemId, amount);
            if(qualification){
                orderService.createOrder(userId, itemId, amount);
            }
        }else{  //活动下单
            //先获取下单资格，若没有资格会抛出对应原因的异常
            qualification = orderService.getOrderQualification(userId, itemId, promoId, amount);
            if(qualification){
                Future<Boolean> future = executorService.submit(() -> {
                    //生成操作库存的日志
                    String stockLogId = UUID.randomUUID().toString().replace("-","");
                    //将库存消息的状态设为未知状态
                    //todo 设置数据库
                    redisTemplate.opsForValue().set("stock_msg_state_"+stockLogId, 0,10, TimeUnit.MINUTES);
                    //发送事务消息,之后执行本地事务
                    return producer.sendTransactionMsg(userId, itemId, promoId, amount, stockLogId);
                });
                try {
                    if(!(Boolean)future.get()){
                        throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
                }
            }
        }

        return CommonReturnType.createSuccessReturn(null);
    }


    //获取当前用户所有订单
    @GetMapping("/list")
    public CommonReturnType listOrder(@RequestParam(name = "token")String token) throws BusinessException {
        if(StringUtils.isEmpty(token)){  //用户未登录
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //从 redis中获取到登录的用户id
        Integer userId = (Integer) redisTemplate.opsForValue().get("token_"+token);
        if(userId == null){  //用户登录信息过期
            throw new BusinessException(EmBusinessError.USER_LOGIN_TIMEOUT);
        }

        List<OrderModel> orderModelList = orderService.getOrderByUser(userId);
        //将 List<ItemModel> -> List<ItemVO>
        List<OrderVO> orderVOList = orderModelList.stream().map(orderModel -> {
            return convertVOFromModel(orderModel);
        }).collect(Collectors.toList());

        return CommonReturnType.createSuccessReturn(orderVOList);
    }


    //支付订单
    @PostMapping("/pay")
    public CommonReturnType payOrder(@RequestParam(name = "orderId")String id,
                                     @RequestParam(name = "token")String token) throws BusinessException {
        if(StringUtils.isEmpty(token)){  //用户未登录
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //从 redis中获取到登录的用户id
        Integer userId = (Integer) redisTemplate.opsForValue().get("token_"+token);
        if(userId == null){  //用户登录信息过期
            throw new BusinessException(EmBusinessError.USER_LOGIN_TIMEOUT);
        }
        orderService.payOrder(id);
        return CommonReturnType.createSuccessReturn(null);
    }


    //取消订单
    @PostMapping("/cancel")
    public CommonReturnType cancelOrder(@RequestParam(name = "orderId")String id,
                                     @RequestParam(name = "token")String token) throws BusinessException {
        if(StringUtils.isEmpty(token)){  //用户未登录
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        //从 redis中获取到登录的用户id
        Integer userId = (Integer) redisTemplate.opsForValue().get("token_"+token);
        if(userId == null){  //用户登录信息过期
            throw new BusinessException(EmBusinessError.USER_LOGIN_TIMEOUT);
        }

        orderService.cancelOrder(id);
        return CommonReturnType.createSuccessReturn(null);
    }



    private OrderVO convertVOFromModel(OrderModel orderModel){
        if(orderModel == null){
            return null;
        }
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orderModel,orderVO);
        try {
            orderVO.setItemName(itemService.getItemById(orderModel.getItemId()).getTitle());
        } catch (BusinessException businessException) {
        }
        orderVO.setCreateTime(orderModel.getCreateTime().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        Integer state = orderModel.getState();
        if(state == 0){
            orderVO.setState("待支付");
        }else if(state == 1){
            orderVO.setState("已支付");
        }else{
            orderVO.setState("已取消");
        }
        return orderVO;
    }

}
