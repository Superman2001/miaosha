package com.deng.miaosha.response;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.CommonError;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CommonReturnType {
    //"success" 或 "fail"
    private String status;

    //若status=="success"，则data内返回前端需要的json数据
    //若status=="fail"，则data内使用通用的错误码格式
    private Object data;


    public static CommonReturnType create(Object result, String status) {
        CommonReturnType ret = new CommonReturnType();
        ret.setStatus(status);
        ret.setData(result);
        return ret;
    }

    /**
     * 成功后返回通用对象
     * @param data
     * @return
     */
    public static CommonReturnType createSuccessReturn(Object data){

        return create(data,"success");
    }

    /**
     * 根据错误码和错误信息返回通用对象
     * @param errCode
     * @param errMsg
     * @return
     */
    public static CommonReturnType createFailReturn(int errCode, String errMsg){

        Map<String,Object> exceptionMap = new HashMap<String,Object>();
        exceptionMap.put("errCode",errCode);
        exceptionMap.put("errMsg",errMsg);

        return create(exceptionMap,"fail");
    }

    /**
     * 根据业务异常返回通用对象
     * @param businessException
     * @return
     */
    public static CommonReturnType createFailReturn(CommonError businessException){
        if(businessException != null){
            return createFailReturn(businessException.getErrCode(),businessException.getErrMsg());
        }
        return create(null,"fail");
    }
}