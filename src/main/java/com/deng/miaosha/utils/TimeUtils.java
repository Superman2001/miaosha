package com.deng.miaosha.utils;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.springframework.stereotype.Component;

@Component
public class TimeUtils {
    //从现在到 endDate有多少秒
    public int secondsBetweenNow(DateTime endDate){
        DateTime now = DateTime.now();
        int seconds = Seconds.secondsBetween(now, endDate).getSeconds();
        return seconds;
    }
}
