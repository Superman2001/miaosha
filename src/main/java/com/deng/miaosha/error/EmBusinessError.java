package com.deng.miaosha.error;

/**
 * 业务错误枚举类
 * 真实企业中由于业务众多，错误码和错误信息可能存放在一个统一的文件中管理，在此用枚举只是模拟真实情况
 */
public enum EmBusinessError implements CommonError{
    //通用错误类型10001

    PARAMETER_VALIDATION_ERROR(10001,"参数不合法"),

    //20000开头为用户信息相关错误定义
    USER_NOT_EXIST(20001,"用户不存在"),
    USER_LOGIN_FAIL(20002,"用户手机号或密码不正确"),
    USER_NOT_LOGIN(20003,"用户还未登录"),
    OTP_CODE_ERROR(20004,"短信验证码不正确"),
    RE_REGISTER_TEL_ERROR(20005,"该手机号已被注册"),
    USER_LOGIN_TIMEOUT(20006,"登录信息已过期"),

    //30000开头为交易信息错误定义
    ITEM_NOT_EXIST(30000,"商品不存在"),
    STOCK_NOT_ENOUGH(30001,"库存不足"),
    PROMO_ERROR(30002,"活动信息不正确"),
    PROMO_NOT_START(30003,"活动还未开始"),
    BUY_AMOUNT_ERROR(30004,"低于起购数量或超过限购数量"),
    STOCK_SELL_OUT(30005,"商品已售完"),

    //40000支付订单错误
    PAYED_ERROR(40001,"订单已支付，请勿重复操作"),
    CANCELED_ERROR(40002,"订单已取消，请勿重复操作"),
    ORDER_NOT_EXIST(40003,"订单不存在"),

    PROMO_BUSY(10000,"当前参与人数过多，请稍后重试"),

    UNKNOWN_ERROR(500500,"未知错误,请联系管理员"),

    ;

    //错误码
    private final int errCode;
    //错误信息
    private final String errMsg;

    EmBusinessError(int errCode,String errMsg){
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }


//    /**
//     * 对于通用错误可能会修改错误信息
//     * 例如,在参数校验不合法时会有不同的错误信息
//     * @param errMsg
//     * @return
//     */
//    //todo 线程是否安全？枚举类是单例，当多个用户同时访问并都参数不合法，会同时修改errMsg
      //可考虑设计为类似不可变类，修改返回新对象，不修改原对象，但不能new枚举类对象
//    @Override
//    public CommonError setErrMsg(String errMsg) {
//        EmBusinessError emBusinessError = new EmBusinessError(this.getErrCode(),errMsg);
//        return emBusinessError;
//    }
}
