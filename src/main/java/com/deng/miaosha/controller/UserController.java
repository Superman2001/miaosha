package com.deng.miaosha.controller;

import com.deng.miaosha.controller.viewobject.UserVO;
import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import com.deng.miaosha.response.CommonReturnType;
import com.deng.miaosha.service.UserService;
import com.deng.miaosha.service.model.UserModel;
import com.deng.miaosha.utils.MD5Utils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private MD5Utils md5Utils;

    @Autowired
    private RedisTemplate redisTemplate;


    //用户登录
    @PostMapping(value = "/login")
    public CommonReturnType login(@RequestParam(name="telphone")String telphone,
                                  @RequestParam(name="password")String password) throws BusinessException,NoSuchAlgorithmException {

        //入参校验,不能为 null或 ""
        if(StringUtils.isEmpty(telphone) || StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //用户登陆服务,用来校验用户登陆是否合法
        UserModel userModel = userService.login(telphone,md5Utils.encodeByMD5(password));

        //登录验证成功后将登录信息和凭证存入redis中
        //生成登录凭证token,使用UUID
        String uuidToken = UUID.randomUUID().toString().replace("-","");

        //将登录凭证token和登录信息存入redis中(将两者绑定),设置有效期为 1小时
        redisTemplate.opsForValue().set("token_"+uuidToken, userModel.getId(), 1, TimeUnit.HOURS);
        redisTemplate.opsForValue().set("user_"+userModel.getId(), userModel, 1, TimeUnit.HOURS);

        return CommonReturnType.createSuccessReturn(uuidToken);
    }


    //获取短信验证码
    @PostMapping("/getotp")
    public CommonReturnType getOtp(String telphone){
        //按照一定规则生成验证码
        //[0,90000)+10000 = [10000,100000),五位随机数
        int randomInt = new Random().nextInt(90000) + 10000;
        String otpCode = String.valueOf(randomInt);

        //将otp验证码和手机号关联存入redis
        redisTemplate.opsForValue().set("register_otp_"+telphone, otpCode, 5, TimeUnit.MINUTES);

        //将otp验证码通过短信发送给用户(模拟)
        System.out.println(telphone + " : " + otpCode);

        return CommonReturnType.createSuccessReturn(null);
    }


    //用户注册
    @PostMapping(value = "/register")
    public CommonReturnType register(@RequestParam(name="telphone")String telphone,
                                     @RequestParam(name="otpCode")String otpCode,
                                     @RequestParam(name="name")String name,
                                     @RequestParam(name="gender")Integer gender,
                                     @RequestParam(name="age")Integer age,
                                     @RequestParam(name="password")String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        //验证手机号和对应的otpCode相符合
        String otpCodeInRedis = (String) redisTemplate.opsForValue().get("register_otp_"+telphone);
        if(!otpCode.equals(otpCodeInRedis)){  //otpCode经过参数绑定后一定不为null
            throw new BusinessException(EmBusinessError.OTP_CODE_ERROR);
        }
        //用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(md5Utils.encodeByMD5(password));
        userService.register(userModel);

        return CommonReturnType.createSuccessReturn(null);
    }


    //根据id获取用户信息
    @GetMapping("/get")
    public CommonReturnType getUser(@RequestParam(name = "id") Integer id) throws BusinessException {
        //获取对应id用户对象
        UserModel userModel = userService.getUserById(id);

        //若用户不存在，抛出异常
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        UserVO userVO = convertFromModel(userModel);
        //返回通用对象
        return CommonReturnType.createSuccessReturn(userVO);
    }


    /**
     * 将领域模型 model 转换为供前端展示的 viewobject
     * @param userModel
     * @return
     */
    private UserVO convertFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel,userVO);
        return userVO;
    }


}
