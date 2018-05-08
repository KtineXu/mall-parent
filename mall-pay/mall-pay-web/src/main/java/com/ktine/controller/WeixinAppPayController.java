package com.ktine.controller;


import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.ImmutableMap;
import com.ktine.pay.dto.HttpResult;
import com.ktine.pay.dto.RestResult;
import com.ktine.service.PayService;
import com.ktine.service.weixinpay.logic.WeixinAppPayLogic;
import com.ktine.util.MapUtils;
import com.ktine.util.OrderUtil;
import com.ktine.util.PayCommonUtil;
import com.ktine.util.PayUtil;
import com.ktine.util.WebUtil;
import com.ktine.util.WeixinConfig;

/**
 * @author ktine
 * @下午8:39:13
 * @desc:APP微信支付入口
 */
@Controller
@RequestMapping("/pay/weixin")
public class WeixinAppPayController {
	
	@Resource
	private PayService payService;

    @Resource
    private WeixinAppPayLogic weixinAppPayLogic ;
    
    /**
     * 微信支付 统一下订单入口
     *
//          * @param ip
//          * @param authorization
//          * @param pro
//          * @param price
//          * @param time
     * @return
     */
    @RequestMapping(value = "preOrder.do", method = RequestMethod.POST)
//    @RequestMapping(value = "preOrder.do")
    public void unifiedOrder(HttpServletResponse response, HttpServletRequest request) throws IOException {
//    	printAllparam(request); //测试代码
    	float  price = 0;
    	try {
        	String temp = request.getParameter("price");
        	System.out.println(temp);
        	if(temp != null ||  temp==""){
        		price = Float.parseFloat(temp);
        		System.out.println("充值："+price);
        		if(price >= 0){
        			String userId = request.getParameter("userId");
        	        String spbill_create_ip = PayUtil.getRemoteAddrIp(request); // 获得终端IP
        	        if(StringUtils.isEmpty(userId)){
        	        	System.out.println("用户登录有问题");
        	        	WebUtil.response(response, RestResult.fail("用户登录有问题").toString());
        	        	return;
        	        }
        	        int currentPrice = (int)(price*100); //将元转换成分
        	        String orderId = OrderUtil.generateOrderId();  //生成订单号
        	        payService.savePreDetail(orderId,userId,"weixinPay",currentPrice); // 保存此次的订单信息
        	        RestResult result = weixinAppPayLogic.unifiedOrder(userId, orderId,spbill_create_ip, currentPrice);
        	        //返回结果
        	        returnMessage (response,result);
        		}
        	}
    	}catch (NumberFormatException e ){
            System.out.println("支付金钱输入有问题");
            WebUtil.response(response, RestResult.fail("支付金钱输入有问题").toString());
        } 
    }
    
   /* private void printAllparam(HttpServletRequest request) {
    	System.out.println("打印微信的请求支付的参数");
		// TODO Auto-generated method stub
    	Map map=request.getParameterMap();
        Set keSet=map.entrySet();
        for(Iterator itr=keSet.iterator();itr.hasNext();){
            Map.Entry me=(Map.Entry)itr.next();
            Object ok=me.getKey();
            Object ov=me.getValue();
            String[] value=new String[1];
            if(ov instanceof String[]){
                value=(String[])ov;
            }else{
                value[0]=ov.toString();
            }
            for(int k=0;k<value.length;k++){
                System.out.println(ok+"="+value[k]);
            }
          }

	}*/
    
    /**
     * 微信回调告诉微信支付结果
     * 注意：同样的通知可能会多次发送给此接口，注意处理重复的通知。
     * 对于支付结果通知的内容做签名验证，防止数据泄漏导致出现“假通知”，造成资金损失。
     *
     * @param request 请求数据
     * @return
     */
//    @RequestMapping(value = "callback.do", method = RequestMethod.POST)
//    public Response callback(HttpRequest request, HttpResponse response) throws IOException {
    @RequestMapping(value = "callback.do", method = RequestMethod.POST)
    public Response callback(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	System.out.println("微信回调接口");
//    	printAllparam(request); //测试接口
        String str = weixinAppPayLogic.callback(request);
        return Response.status(200).entity(str).build();
    }

	/**
     * 查询订单接口 该接口提供所有微信支付订单的查询， 商户可以通过该接口主动查询订单状态， 完成下一步的业务逻辑。
     *
//     * @param outTradeNo    商户订单号
//     * @param transactionId 微信订单号
     * @return
     */
    @RequestMapping(value ="queryOrder.do",method = RequestMethod.POST)

    /**
     *  outTradeNo 商户订单号
     *  transactionId 微信订单号
     */
    public Response queryOrder(HttpServletResponse response, HttpServletRequest request) throws IOException {

        try {
            String  outTradeNo = "";
            String transactionId= "";
//            logger.info("weixin_pay unifiedOrder: " + authorization + "-"
//                    + outTradeNo + "-" + transactionId);
            SortedMap<String, Object> params = prepareQueryData(outTradeNo,
                    transactionId);
//            logger.debug("查询订单的请求数据 ={}" + JSONObject.toJSONString(params));
//            // 调用查询订单接口
            HttpResult<String> result = weixinAppPayLogic.checkOrderStatus(params);
//            logger.debug("结束调用微信订单查询接口...");
            return Response.status(200).entity(result).build();
        } catch (Exception e) {
//            logger.error("结束调用微信订单查询接口...{} {}", outTradeNo + "-"
//                    + transactionId, e.getMessage());
            RestResult result = RestResult.fail("查询失败");
            return Response.status(200).entity(result).build();
        }
    }

    private void returnMessage(HttpServletResponse response, RestResult restResult) {
    	
        String strJson = restResult.toString(); 
        System.out.println(strJson);
        try {
            response.getWriter().write(strJson);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if( response.getWriter() != null){
                    response.getWriter().close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

	/**
     * 封装查询请求数据
     * @param outTradeNo
     * @param transactionId
     * @return
     */
    private SortedMap<String, Object> prepareQueryData(String outTradeNo,
                                                       String transactionId) {
        Map<String, Object> queryParams = null;
        // 微信的订单号，优先使用
        if (null == outTradeNo || outTradeNo.length() == 0) {
            queryParams = ImmutableMap
                    .<String, Object> builder()
                    .put("appid",WeixinConfig.getAppId())
                    .put("mch_id",WeixinConfig.getMchId())
                    .put("transaction_id", transactionId)
                    .put("nonce_str", PayCommonUtil.CreateNoncestr())
                    .build();
        } else {
            queryParams = ImmutableMap
                    .<String, Object> builder()
                    .put("appid", WeixinConfig.getAppId())
                    .put("mch_id", WeixinConfig.getMchId())
                    .put("out_trade_no", outTradeNo)
                    .put("nonce_str", PayCommonUtil.CreateNoncestr())
                    .build();
        }
        // key ASCII 排序
        SortedMap<String, Object> sortMap = MapUtils.sortMap(queryParams);
        // MD5签名
        String createSign = PayCommonUtil.createSign("UTF-8", sortMap);
        sortMap.put("sign", createSign);
        return sortMap;
    }

}