package com.ktine.pay.dto;

import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.lang3.StringUtils;

import net.sf.json.JSONObject;

public class RestResult {
	
    private static String message = "";//返回给客户端的消息
    // 1 代表成功,-1 代表失败
    private static Map<String ,Object> result ;//数据
    private int code = -1;//错误码
	public RestResult() {
    }
    private RestResult(Map<String, Object> result, int code, String msg) {
        this.result = result;
        this.code = code;
        this.message = msg;
    }
    private RestResult(int code, String msg) {
        this.code = code;
        this.message = msg;
    }
    /**
     * 充值失败
     *
     * @param message 错误信息
     * @return
     */
    public static RestResult fail(String message) {

        return new RestResult (-1,message);
    }

    public static RestResult OK(SortedMap<String, Object> map) {
        return new RestResult(map,1,null);
    }
	public static String getMessage() {
		return message;
	}
	public static void setMessage(String message) {
		RestResult.message = message;
	}
	public static Map<String, Object> getResult() {
		return result;
	}
	public static void setResult(Map<String, Object> result) {
		RestResult.result = result;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
    	sb.append( "{\"code\":" +  code );
        if(StringUtils.isNotEmpty(message)){
        	sb.append( ",\"message\":\"" + message +"\"");
        }
        if( result!= null &&  !result.isEmpty()){
        	sb.append(",\"body\":"+JSONObject.fromObject(result).toString());
        } 
        sb.append( "}");
		return sb.toString();
	}
}
