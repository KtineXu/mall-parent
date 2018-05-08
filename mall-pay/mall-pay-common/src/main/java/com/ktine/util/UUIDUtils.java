package com.ktine.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class UUIDUtils {

    public static  String generateOrderId(){
        //格式化当前时间
        SimpleDateFormat sfDate = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        //得到17位时间如：20170411094039080
        String strDate = sfDate.format(new Date()).trim();
        //为了防止高并发重复,再获取3个随机数
        String random = getRandomNum(3).trim();
        //最后得到20位订单编号。
        System.out.println("订单号20位:" + strDate + random);
        return strDate+random;
    }
    /**
     * 获取6-10 的随机位数数字
     * @param length	想要生成的长度
     * @return result
     */
    public static String getRandomNum(Integer length) {
        String result = "";
        Random rand = new Random();
        int n = 20;
        if (null != length && length > 0) {
            n = length;
        }
        int randInt = 0;
        for (int i = 0; i < n; i++) {
            randInt = rand.nextInt(10);
            result += randInt;
        }
        return result;
    }
}

