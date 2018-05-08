package com.ktine.pay.dto;

import java.util.SortedMap;

public class HttpResult<T> {


    private static String message = "";//返回给客户端的消息
    // 1 代表成功
    // -1 代表失败
    private static Object result = "";//数据
    private int code = -1;//错误码

    public HttpResult() {
    }

    private HttpResult(Object result, int code, String msg) {
        this.result = result;
        this.code = code;
        this.message = msg;
    }

    public HttpResult(int code, String msg) {
        this.code = code;
        this.message = msg;
    }


    public static HttpResult<String> error(int i, String message) {
        return new HttpResult (i,message);
    }

    public static HttpResult<String> success(int i, String 订单查询失败) {
        return new HttpResult (i,message);
    }
}
