package com.deng.miaosha.controller;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import com.deng.miaosha.response.CommonReturnType;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolationException;

/**
 * 处理从controller层抛出的所有异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 处理业务异常
     * @param e
     * @return
     */
    @ExceptionHandler(value = BusinessException.class)
    public CommonReturnType handleBusinessException(BusinessException e){
        e.printStackTrace();//调试
        return CommonReturnType.createFailReturn(e);
    }

    /**
     * 处理数据校验异常(JSR303校验失败后可能会抛出的三种异常)
     * @param e
     * @return
     */
    @ExceptionHandler(value = {BindException.class})
    public CommonReturnType handleValidException(BindException e) {
        e.printStackTrace();//调试
        return CommonReturnType.createFailReturn(EmBusinessError.PARAMETER_VALIDATION_ERROR.getErrCode(),
                EmBusinessError.PARAMETER_VALIDATION_ERROR.getErrMsg());
    }
    @ExceptionHandler(value = {ConstraintViolationException.class})
    public CommonReturnType handleValidException(ConstraintViolationException e) {
        e.printStackTrace();//调试
        return CommonReturnType.createFailReturn(EmBusinessError.PARAMETER_VALIDATION_ERROR.getErrCode(),
                EmBusinessError.PARAMETER_VALIDATION_ERROR.getErrMsg());
    }
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public CommonReturnType handleValidException(MethodArgumentNotValidException e) {
        e.printStackTrace();//调试
        return CommonReturnType.createFailReturn(EmBusinessError.PARAMETER_VALIDATION_ERROR.getErrCode(),
                EmBusinessError.PARAMETER_VALIDATION_ERROR.getErrMsg());
    }


    /**
     * 处理请求参数绑定异常
     * @param e
     * @return
     */
    @ExceptionHandler(value = {MethodArgumentTypeMismatchException.class,ServletRequestBindingException.class})
    public CommonReturnType handleBindingException(ServletRequestBindingException e){
        e.printStackTrace();//调试
        return CommonReturnType.createFailReturn(400400," 请求无效,请求参数绑定错误");
    }

    /**
     * 处理404异常
     * @param e
     * @return
     */
    @ExceptionHandler(value = NoHandlerFoundException.class)
    public CommonReturnType handleNotFoundException(NoHandlerFoundException e){
        e.printStackTrace();//调试
        return CommonReturnType.createFailReturn(404404,"未找到页面");
    }

    /**
     * 处理其他异常
     * @param e
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public CommonReturnType handleOtherException(Exception e){
        e.printStackTrace();//调试
        return CommonReturnType.createFailReturn(500500,"未知错误,请联系管理员");
    }
}
