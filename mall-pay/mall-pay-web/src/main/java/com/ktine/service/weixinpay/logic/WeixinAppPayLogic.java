package com.ktine.service.weixinpay.logic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.SortedMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.chemguan.utils.HttpUtil;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.ktine.pay.dto.HttpResult;
import com.ktine.pay.dto.RestResult;
import com.ktine.service.PayService;
import com.ktine.util.DateUtils;
import com.ktine.util.MapUtils;
import com.ktine.util.PayCommonUtil;
import com.ktine.util.WeixinConfig;
import com.ktine.util.WeixinConstant;
import com.ktine.util.XMLUtil;


@Service
public class WeixinAppPayLogic {

    private static String DEFAULTIP = "8.8.8.8";

    private static final Logger logger = LoggerFactory.getLogger(WeixinAppPayLogic.class);
    
    @Resource
	private PayService payService;
    
    /**
     * 微信预支付 统一下单入口
     *
     * @author wangkai
     * @param userId
     *            用户id
     * @param proId
     *            商品id
     * @return
     */
    public RestResult unifiedOrder(String userId, String orderId,String ip,
                                   int price) {
        try {
            System.out.println("统一下定单开始 用户："+userId+" ip:"+ip+" 金额:"+price);
            if(ip == null ||  ip.equals("")){
                ip=DEFAULTIP;
            }
            // 设置订单参数
            //咪咕体验会员 支付1角购买20秒
            logger.info("weixin unify order price:{},proId:{}",price);
            SortedMap<String, Object> parameters = prepareOrder(ip, orderId,price);
            parameters.put("sign",
                    PayCommonUtil.createSign( Charsets.UTF_8.toString(), parameters));// sign签名 key
            String requestXML = PayCommonUtil.getRequestXml(parameters);// 生成xml格式字符串
            String responseStr = HttpUtil.httpsRequest(
                    WeixinConfig.getUnifiedOrderUrl(), "POST", requestXML);// 带上post
            // 检验API返回的数据里面的签名是否合法，避免数据在传输的过程中被第三方篡改
            if (!PayCommonUtil.checkIsSignValidFromResponseString(responseStr)) {
//                Logger.error("微信统一下单失败,签名可能被篡改 "+responseStr);
                return RestResult.fail("统一下单失败");
            } else {
				System.out.println("调用微信接口成功");
			}
            // 解析结果 resultStr
            SortedMap<String, Object> resutlMap = XMLUtil.doXMLParse(responseStr);
            if (resutlMap != null
                    && WeixinConstant.FAIL.equals(resutlMap.get("return_code"))) {
//                LoggerUtils.payLogger.error("微信统一下单失败,订单编号: " + orderId + " 失败原因:"
//                        + resutlMap.get("return_msg"));
            	logger.error("微信统一下单失败,订单编号: " + orderId + " 失败原因:"
                       + resutlMap.get("return_msg"));
            	System.out.println("微信统一下单失败,订单编号: " + orderId + " 失败原因:"
                       + resutlMap.get("return_msg"));
                return RestResult.fail("统一下单失败");
            }
            // 获取到 prepayid
            // 商户系统先调用该接口在微信支付服务后台生成预支付交易单，返回正确的预支付交易回话标识后再在APP里面调起支付。
            SortedMap<String, Object> map = buildClientJson(resutlMap);
            map.put("outTradeNo", orderId);
//            LoggerUtils.payLogger.info("统一下定单成功 "+map.toString());
            return RestResult.OK(map);
        } catch (Exception e) {
//            LoggerUtils.payLogger.error(
//                    "下订单异常com.fs.module.weixin.logic.WeixinLogic receipt(String userId,String proId,String ip)：{},{}",
//                    "用户："+userId + " 商品号:" + proId + " IP：" + ip, "失败原因"+e.getMessage());
        	System.out.println("下订单异常com.fs.module.weixin.logic.WeixinLogic receipt(String userId,String proId,String ip)"+
                    "用户："+userId + " IP：" + ip + "失败原因"+e.getMessage());
            return RestResult.fail("预支付请求失败"); // 抽离到统一错误码泪中 统一定一下
        }

    }

    /**
     * 生成订单信息
     *
     * @param ip
     * @param orderId
     * @return
     */
    private SortedMap<String, Object> prepareOrder(String ip, String orderId,
                                                   int price) {
        Map<String, Object> oparams = ImmutableMap.<String, Object> builder()
                .put("appid",WeixinConfig.getAppId())// 服务号的应用号
                .put("body", WeixinConstant.PRODUCT_BODY)// 商品描述
                .put("mch_id", WeixinConfig.getMchId())// 商户号 ？
                .put("nonce_str", PayCommonUtil.CreateNoncestr())// 16随机字符串(大小写字母加数字)
                .put("out_trade_no", orderId)// 商户订单号
                .put("total_fee", price)// 支付金额 单位分 注意:前端负责传入分
                .put("spbill_create_ip", ip)// IP地址
                .put("notify_url", WeixinConfig.getPayCallbackUrl()) // 微信回调地址
                .put("trade_type", WeixinConfig.getTradeType())// 支付类型 app
                .build();
        logger.info("weixin pay notify_url:{}",WeixinConfig.getPayCallbackUrl());
        return MapUtils.sortMap(oparams);
    }

    /**
     * 生成预付快订单完成，返回给android,ios唤起微信所需要的参数。
     *
     * @param resutlMap
     * @return
     * @throws UnsupportedEncodingException
     */
    private SortedMap<String, Object> buildClientJson(
            Map<String, Object> resutlMap) throws UnsupportedEncodingException {
        // 获取微信返回的签名
        Map<String, Object> params = ImmutableMap.<String, Object> builder()
                .put("appid", WeixinConfig.getAppId())
                .put("noncestr", PayCommonUtil.CreateNoncestr())
                .put("package", "Sign=WXPay")
                .put("partnerid", WeixinConfig.getMchId())
                .put("prepayid", resutlMap.get("prepay_id"))
                .put("timestamp", DateUtils.getTimeStamp()) // 10 位时间戳
                .build();
        // key ASCII排序 // 这里用treemap也是可以的 可以用treemap // TODO
        SortedMap<String, Object> sortMap = MapUtils.sortMap(params);
        sortMap.put("package", "Sign=WXPay");
        // paySign的生成规则和Sign的生成规则同理
        String paySign = PayCommonUtil.createSign(Charsets.UTF_8.toString(), sortMap);
        sortMap.put("sign", paySign);
        return sortMap;
    }

    /**
     * 微信回调告诉微信支付结果 注意：同样的通知可能会多次发送给此接口，注意处理重复的通知。
     * 对于支付结果通知的内容做签名验证，防止数据泄漏导致出现“假通知”，造成资金损失。
     *
     * @param request
     * @return
     */
    public String callback(HttpServletRequest request) {
        try {
            String responseStr = parseWeixinCallback(request);
            //测试
            if(responseStr != null ){
            	System.out.println(responseStr);
            } else {
            	System.out.println("callback(HttpServletRequest request)：没有获得参数");
            }
            Map<String, Object> map = XMLUtil.doXMLParse(responseStr);
//            LoggerUtils.payLogger.info("微信支付回调: "+map.toString());
          //测试
            if(map != null ){
            	System.out.println(map.toString());
            } else {
            	System.out.println("回调参数解析失败");
            }
            
            // 校验签名 防止数据泄漏导致出现“假通知”，造成资金损失
            if (!PayCommonUtil.checkIsSignValidFromResponseString(responseStr)) {
//                LoggerUtils.payLogger.error("微信回调失败,签名可能被篡改 "+responseStr);
                return PayCommonUtil.setXML(WeixinConstant.FAIL, "invalid sign");
            }
            System.out.println("微信支付的结果："+map.get("result_code"));
            if (WeixinConstant.FAIL.equalsIgnoreCase(map.get("result_code")
                    .toString())) {
//                LoggerUtils.payLogger.error("微信回调失败的原因："+responseStr);
            	//更新支付失败的充值列表项
            	 String outTradeNo = (String) map.get("out_trade_no");
            	updateFailPay(outTradeNo);
                return PayCommonUtil.setXML(WeixinConstant.FAIL, "weixin pay fail");
            }
            if (WeixinConstant.SUCCESS.equalsIgnoreCase(map.get("result_code")
                    .toString())) {
            	
                // 对数据库的操作
                String outTradeNo = (String) map.get("out_trade_no");
                String transactionId = (String) map.get("transaction_id");
                String totlaFee = (String) map.get("total_fee");
                
                //打印回掉支付结果
                System.out.println("outTradeNo = " + outTradeNo); 
                System.out.println("transactionId = " + transactionId); 
                System.out.println("totlaFee = " + totlaFee); 
                
                Integer totalPrice = Integer.valueOf(totlaFee);//服务器这边记录的是钱的分
                System.out.println("微信支付的金额是："+totalPrice+"分");
                boolean isOk = payService.updateDepositDetail(outTradeNo, totalPrice);
                logger.info("outTradeNo:"+outTradeNo +" price:" + totalPrice);
            	System.out.println("outTradeNo:"+outTradeNo +" price:" + totalPrice );
                // 告诉微信服务器，我收到信息了，不要在调用回调action了
                logger.info("回调成功："+responseStr);
                if (isOk) {
                    return PayCommonUtil.setXML(WeixinConstant.SUCCESS, "OK");
                } else {
                    return PayCommonUtil
                            .setXML(WeixinConstant.FAIL, "update bussiness outTrade fail");
                }
                
            }
        } catch (Exception e) {
//            LoggerUtils.payLogger.error("回调异常" + e.getMessage());
            return PayCommonUtil.setXML(WeixinConstant.FAIL,
                    "weixin pay server exception");
        }
        return PayCommonUtil.setXML(WeixinConstant.FAIL, "weixin pay fail");
    }

	/**
     * 解析微信回调参数
     *
     * @param request
     * @return
     * @throws IOException
     */
    private String parseWeixinCallback(HttpServletRequest request) throws IOException {
        // 获取微信调用我们notify_url的返回信息
        String result = "";
        InputStream inStream = request.getInputStream();
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
            }
            result = new String(outSteam.toByteArray(), Charsets.UTF_8.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            try {
                if(outSteam != null){
                    outSteam.close();
                    outSteam = null; // help GC
                }
                if(inStream != null){
                    inStream.close();
                    inStream = null;// help GC
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 查询订单状态
     *
     * @param params
     *            订单查询参数
     * @return
     */
    public HttpResult<String> checkOrderStatus(SortedMap<String, Object> params) {
        if (params == null) {
            return HttpResult.error(1, "查询订单参数不能为空");
        }
        try {
            String requestXML = PayCommonUtil.getRequestXml(params);// 生成xml格式字符串
            String responseStr = HttpUtil.httpsRequest(
                    WeixinConfig.getCheckOrderUrl(), "POST", requestXML);// 带上post
            SortedMap<String, Object> responseMap = XMLUtil
                    .doXMLParse(responseStr);// 解析响应xml格式字符串

            // 校验响应结果return_code
            if (WeixinConstant.FAIL.equalsIgnoreCase(responseMap.get(
                    "return_code").toString())) {
                return HttpResult.error(1, responseMap.get("return_msg")
                        .toString());
            }
            // 校验业务结果result_code
            if (WeixinConstant.FAIL.equalsIgnoreCase(responseMap.get(
                    "result_code").toString())) {
                return HttpResult.error(2, responseMap.get("err_code")
                        .toString() + "=" + responseMap.get("err_code_des"));
            }
            // 校验签名
            if (!PayCommonUtil.checkIsSignValidFromResponseString(responseStr)) {
//                LoggerUtils.payLogger.error("订单查询失败,签名可能被篡改："+responseStr);
                return HttpResult.error(3, "签名错误");
            }
            // 判断支付状态
            String tradeState = responseMap.get("trade_state").toString();
            if (tradeState != null && tradeState.equals("SUCCESS")) {
                return HttpResult.success(0, "订单支付成功");
            } else if (tradeState == null) {
                return HttpResult.error(4, "获取订单状态失败");
            } else if (tradeState.equals("REFUND")) {
                return HttpResult.error(5, "转入退款");
            } else if (tradeState.equals("NOTPAY")) {
                return HttpResult.error(6, "未支付");
            } else if (tradeState.equals("CLOSED")) {
                return HttpResult.error(7, "已关闭");
            } else if (tradeState.equals("REVOKED")) {
                return HttpResult.error(8, "已撤销（刷卡支付)");
            } else if (tradeState.equals("USERPAYING")) {
                return HttpResult.error(9, "用户支付中");
            } else if (tradeState.equals("PAYERROR")) {
                return HttpResult.error(10, "支付失败");
            } else {
                return HttpResult.error(11, "未知的失败状态");
            }
        } catch (Exception e) {
//            LoggerUtils.payLogger.error("订单查询失败,查询参数 = {}", JSONObject.toJSONString(params));
            return HttpResult.success(1, "订单查询失败");
        }
    }

    /**
     * 
     * @param outTradeNo
     */
    private void updateFailPay(String outTradeNo) {
		// TODO Auto-generated method stub
		
	}
    
}
