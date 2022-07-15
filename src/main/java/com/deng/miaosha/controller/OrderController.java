package com.deng.miaosha.controller;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import com.deng.miaosha.response.CommonReturnType;
import com.deng.miaosha.service.OrderService;
import com.deng.miaosha.service.model.OrderModel;
import com.deng.miaosha.service.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisTemplate redisTemplate;

    //创建订单
    //通过前端url上传过来秒杀活动id，然后下单接口内校验对应id是否属于对应商品且活动已开始
    @PostMapping("/createorder")
    public CommonReturnType createOrder(@RequestParam(name = "itemId")Integer itemId,
                                        @RequestParam(name = "promoId",required = false)Integer promoId,
                                        @RequestParam(name = "amount")Integer amount,
                                        @RequestParam(name = "token")String token) throws BusinessException {
//        //从Session中获取到登录的用户
//        UserModel userModel = (UserModel) httpServletRequest.getSession().getAttribute("LOGIN_USER");

        if(StringUtils.isEmpty(token)){  //用户未登录
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }

        //从 redis中获取到登录的用户id
        Integer userId = (Integer) redisTemplate.opsForValue().get("token_"+token);

        if(userId == null){  //用户登录信息过期
            throw new BusinessException(EmBusinessError.USER_LOGIN_TIMEOUT);
        }

        OrderModel orderModel = orderService.createOrder(userId, itemId, promoId, amount);

        return CommonReturnType.createSuccessReturn(null);
    }

}
