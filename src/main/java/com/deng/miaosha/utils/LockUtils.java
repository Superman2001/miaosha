package com.deng.miaosha.utils;

import com.deng.miaosha.error.BusinessException;
import com.deng.miaosha.error.EmBusinessError;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class LockUtils {
    @Autowired
    private RedisTemplate redisTemplate;

    //释放锁的lua脚本
    private static final String RELEASE_LOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then"+
            "   return redis.call('del', KEYS[1]);"+
            "else return 0;"+
            "end;";


    //加分布式锁，只尝试一次，在成功时return 锁的value，失败时return null
    public String lock(String key){
        String value = UUID.randomUUID().toString().replace("-","");
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, value, 30, TimeUnit.SECONDS);
        if(flag != null && flag){
            return value;
        }
        return null;
    }


    //加分布式锁,重复尝试加锁，直到成功
    public String tryLock(String key) throws BusinessException {
        String lockValue = lock(key);
        //循环重试加锁（每秒重试1次,重试30次）
        for(int i = 0; i < 30 && lockValue == null; i++){
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            lockValue = lock(key); //重新加锁
        }
        if(lockValue == null){ //最终加锁失败
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        return lockValue;
    }


    //解分布式锁
    public Boolean unlock(String key, String value){
        // lua脚本里的KEYS参数
        List<String> keys = new ArrayList<>();
        keys.add(key);
        // lua脚本里的ARGV参数
        List<String> args = new ArrayList<>();
        args.add(value);

        return (Boolean) redisTemplate.execute((RedisCallback<Boolean>) redisConnection -> {
            Jedis jedis = (Jedis) redisConnection.getNativeConnection(); //单机版redis
            Object result = jedis.eval(RELEASE_LOCK_LUA, keys, args);
            return Long.valueOf(1L).equals(result);
        });
    }
}
