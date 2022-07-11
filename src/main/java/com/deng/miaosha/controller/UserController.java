package com.deng.miaosha.controller;

import com.deng.miaosha.controller.viewobject.UserVO;
import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import com.deng.miaosha.response.CommonReturnType;
import com.deng.miaosha.service.UserService;
import com.deng.miaosha.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    //经过spring包装的HttpServletRequest对象是在ThreadLocal中，每个请求（每个线程）之间互不干扰,
    // 与在controller方法参数中加HttpServletRequest参数效果相同
    @Autowired
    private HttpServletRequest httpServletRequest;

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
        UserModel userModel = userService.login(telphone,this.EncodeByMd5(password));

//        //将登陆信息加入到用户登陆成功的session内
//        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);

        //登录验证成功后将登录信息和凭证存入redis中
        //生成登录凭证token,使用UUID
        String uuidToken = UUID.randomUUID().toString().replace("-","");

        //将登录凭证token和登录信息存入redis中(将两者绑定)
        redisTemplate.opsForValue().set("token_"+uuidToken, userModel);
        //设置缓存有效期为 1小时
        redisTemplate.expire("token_"+uuidToken,1, TimeUnit.HOURS);

        return CommonReturnType.createSuccessReturn(uuidToken);
    }


    //获取短信验证码
    @PostMapping("/getotp")
    public CommonReturnType getOtp(String telphone){
        //按照一定规则生成验证码
        //[0,90000)+10000 = [10000,100000),五位随机数
        int randomInt = new Random().nextInt(90000) + 10000;
        String otpCode = String.valueOf(randomInt);

        //将otp验证码和手机号关联
        httpServletRequest.getSession().setAttribute(telphone,otpCode);

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
        String inSessionOtpCode = (String) httpServletRequest.getSession().getAttribute(telphone);
        if(!otpCode.equals(inSessionOtpCode)){  //otpCode经过参数绑定后一定不为null
            throw new BusinessException(EmBusinessError.OTP_CODE_ERROR);
        }
        //用户的注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(new Byte(String.valueOf(gender.intValue())));
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setEncrptPassword(this.EncodeByMd5(password));
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

    //todo 放到一个通用的工具类中
    private String EncodeByMd5(String str) throws NoSuchAlgorithmException{
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64en = new BASE64Encoder();
        //加密字符串
        String newstr = base64en.encode(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
        return newstr;
    }
}
