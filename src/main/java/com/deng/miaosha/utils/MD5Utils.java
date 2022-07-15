package com.deng.miaosha.utils;

import org.springframework.stereotype.Component;
import sun.misc.BASE64Encoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class MD5Utils {
    public String encodeByMD5(String str) throws NoSuchAlgorithmException {
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64en = new BASE64Encoder();
        //加密字符串
        String newStr = base64en.encode(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
        return newStr;
    }
}
