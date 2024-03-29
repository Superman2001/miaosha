package com.deng.miaosha.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

//用于操作本地缓存
//todo 使用 guava提供的本地缓存 和 CurrentHashMap+LRU策略实现的本地缓存 的区别
@Component
public class LocalCache {

    private Cache<String,Object> commonCache = null;

    @PostConstruct
    public void init(){
        commonCache = CacheBuilder.newBuilder()
                //设置缓存容器的初始容量为10
                .initialCapacity(10)
                //设置缓存中最大可以存储100个KEY,超过100个之后会按照LRU的策略移除缓存项
                .maximumSize(100)
                //设置写缓存后多少秒过期
                .expireAfterWrite(60, TimeUnit.SECONDS).build();
    }

    //存
    public void setCommonCache(String key, Object value) {
        commonCache.put(key,value);
    }

    //取
    public Object getFromCommonCache(String key) {
        return commonCache.getIfPresent(key);
    }
}
