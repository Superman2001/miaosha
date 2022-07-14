package com.deng.miaosha.service;

//用于操作本地缓存
public interface CacheService {
    //存方法
    void setCommonCache(String key, Object value);

    //取方法
    Object getFromCommonCache(String key);
}
