package com.deng.miaosha.utils;

import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TimeUtils {
    //从现在到 endDate有多少秒
    public static int secondsBetweenNow(DateTime endDate){
        DateTime now = DateTime.now();
        return Seconds.secondsBetween(now, endDate).getSeconds();
    }

    public static DateTime convertDateToDateTime(Date date){
        return new DateTime(date);
    }

    public static Date convertDateTimeToDate(DateTime dateTime){
        return dateTime.toDate();
    }

}
