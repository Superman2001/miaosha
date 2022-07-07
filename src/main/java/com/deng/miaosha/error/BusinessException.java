package com.deng.miaosha.error;


/**
 * 将业务错误作为异常抛出：
 * 利用装饰器模式（包装器模式）将业务错误枚举类包装为异常类
 */
// 异常类不能设计成单例，因为每个异常对象都有不同的堆栈(抛出位置不同)
public class BusinessException extends Exception implements CommonError{

    private final CommonError commonError;

    //直接接收EmBusinessError的传参用于构造业务异常
    public BusinessException(CommonError commonError){
        super();
        this.commonError = commonError;
    }

//    //接收自定义errMsg的方式构造业务异常
//    public BusinessException(CommonError commonError,String errMsg){
//        super();
//        this.commonError = commonError;
//        this.commonError.setErrMsg(errMsg);
//    }

    @Override
    public int getErrCode() {
        return this.commonError.getErrCode();
    }

    @Override
    public String getErrMsg() {
        return this.commonError.getErrMsg();
    }

//    @Override
//    public CommonError setErrMsg(String errMsg) {
//        this.commonError.setErrMsg(errMsg);
//        return this;
//    }

    public CommonError getCommonError() {
        return commonError;
    }
}
