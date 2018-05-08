package com.ktine.controller;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayConstants;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.ktine.model.JsonResult;
import com.ktine.model.ResponseData;
import com.ktine.pay.dto.RestResult;
import com.ktine.service.PayService;
import com.ktine.util.AlipayConfig;
import com.ktine.util.DatetimeUtil;
import com.ktine.util.OrderUtil;
import com.ktine.util.PayUtil;
import com.ktine.util.SerializerFeatureUtil;
import com.ktine.util.StringUtil;
import com.ktine.util.WebUtil;

@Controller
@RequestMapping("/pay/alipay")
public class AliPayController {

	@Resource
	private PayService payService;

	/**
	 * 支付下订单
	 * 
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "preOrder.do", method = RequestMethod.POST)
	public void orderPay(HttpServletRequest request, HttpServletResponse response) {
		// 需要支付金额,订单号
		String userId = request.getParameter("userId");
		if (userId == null || userId.equals("")) {
			WebUtil.response(response, RestResult.fail("用户登录失效").toString());
		}
		// 获得终端IP
		float price = 0;
		try {
			String temp = request.getParameter("price");
			System.out.println(temp);
			if (temp != null || temp == "") {
				price = Float.parseFloat(temp);
				System.out.println("充值：" + price);
				if (price >= 0) {
					// 生成订单号
					String orderId = OrderUtil.generateOrderId();
					String callback = AlipayConfig.getCallBackUrl();
					// 用户登录验证,加入用户登录拦截器
					// 保存此次的订单信息
					payService.savePreDetail(orderId, userId, "aliPay", (int) (price * 100));
					Map<String, String> param = new HashMap<>();
					// 公共请求参数
					param.put("app_id", AlipayConfig.getPartnerId());// 支付宝分配给开发者的应用ID
					param.put("method", "alipay.trade.app.pay");// 接口名称
					param.put("format", AlipayConstants.FORMAT_JSON); // 仅支持JSON
					param.put("charset", AlipayConstants.CHARSET_UTF8); // 编码格式
					param.put("timestamp", DatetimeUtil.formatDateTime(new Date())); // 发送请求的时间
					param.put("version", "1.0");
					param.put("notify_url", AlipayConfig.getNotifyUrl()); // 支付宝服务器主动通知商户服务器里指定的页面
					param.put("sign_type", AlipayConstants.SIGN_TYPE_RSA);
					Map<String, Object> pcont = new HashMap<>();
					// 支付业务请求参数
					pcont.put("out_trade_no", orderId); // 商户网站唯一订单号
					pcont.put("subject", "充值"); // 商品的标题/交易标题/订单标题/订单关键字等。
					pcont.put("body", "test");// 对交易或商品的描述
					pcont.put("product_code", "QUICK_MSECURITY_PAY");// 销售产品码，商家和支付宝签约的产品码
					pcont.put("total_amount", temp); // 订单总金额
					pcont.put("timeout_express", "30m"); // 该笔订单允许的最晚付款时间
					param.put("biz_content", JSON.toJSONString(pcont)); // 业务请求参数
																		// 不需要对json字符串转义
					Map<String, String> payMap = new HashMap<>();
					try {
						param.put("sign", PayUtil.getSign(param, AlipayConfig.getPrivateKey())); // 业务请求参数
						payMap.put("orderStr", PayUtil.getSignEncodeUrl(param, true));
					} catch (Exception e) {
						e.printStackTrace();
					}
					WebUtil.response(response,
							WebUtil.packJsonp(callback,
									JSON.toJSONString(new JsonResult(1, "订单获取成功", new ResponseData(null, payMap)),
											SerializerFeatureUtil.FEATURES)));
				}
			} else {
				System.out.println("支付金钱输入小于0 ");
				WebUtil.response(response, RestResult.fail("支付金钱输入有问题").toString());
			}
		} catch (NumberFormatException e) {
			System.out.println("支付金钱输入有问题");
			WebUtil.response(response, RestResult.fail("支付金钱输入有问题").toString() );
		}
	}

	/**
	 * 订单支付微信服务器异步通知
	 *
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "callback.do", method = RequestMethod.POST)
	public void callback(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// 获取到返回的所有参数 先判断是否交易成功trade_status 再做签名校验
		// 1、商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
		// 2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
		// 3、校验通知中的seller_id（或者seller_email)
		// 是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email），
		// 4、验证app_id是否为该商户本身。上述1、2、3、4有任何一个验证不通过，则表明本次通知是异常通知，务必忽略。在上述验证通过后商户必须根据支付宝不同类型的业务通知，正确的进行不同的业务处理，并且过滤重复的通知结果数据。在支付宝的业务通知中，只有交易通知状态为TRADE_SUCCESS或TRADE_FINISHED时，支付宝才会认定为买家付款成功。
		System.out.println("支付宝调用回调");
		if ("TRADE_SUCCESS".equals(request.getParameter("trade_status"))) {
			Enumeration<?> pNames = request.getParameterNames();
			Map<String, String> param = new HashMap<String, String>();
			try {
				while (pNames.hasMoreElements()) {
					String pName = (String) pNames.nextElement();
					param.put(pName, request.getParameter(pName));
					System.out.println(pName + "====" + request.getParameter(pName));
				}
				boolean signVerified = AlipaySignature.rsaCheckV2(param, AlipayConfig.getPublicKey(),
						AlipayConstants.CHARSET_UTF8); // 校验签名是否正确
				if (!signVerified) {
					// TODO 验签成功后
					// 按照支付结果异步通知中的描述，对支付结果中的业务内容进行1\2\3\4二次校验，校验成功后在response中返回success，校验失败返回failure
					// LOG.info("订单支付成功：" + JSON.toJSONString(param));
					// 订单号
					String out_trade_no = param.get("out_trade_no");
					// 获得付款的金额
					String total_amount = param.get("total_amount");
					// seller_id
					String seller_id = param.get("seller_id");
					// 判断app_id是否是本商家id
					String app_id = param.get("app_id");
					// 进行逻辑，校验是否支付成功
					System.out.println("签名检测成功");
					printParam("out_trade_no", out_trade_no);
					printParam("total_amount", total_amount);
					printParam("seller_id", seller_id);
					printParam("app_id", app_id);
					try {
						payService.updateDepositDetail(out_trade_no, (int) (Float.parseFloat(total_amount) * 100));
					} catch (NumberFormatException e) {
						// 记录回调支付失败异常
						e.printStackTrace();
						System.out.println("记录回调支付失败异常");
					}
				} else {
					// TODO 验签失败则记录异常日志，并在response中返回failure.
					System.out.println("回调签名有问题");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void printParam(String string, String out_trade_no) {
		// TODO Auto-generated method stub
		System.out.println(string + "  " + out_trade_no);
	}

	/**
	 * 订单退款
	 *
	 * @param request
	 * @param response
	 * @param tradeno
	 *            支付宝交易订单号
	 * @param orderno
	 *            商家交易订单号
	 * @param callback
	 */
	@RequestMapping(value = "refund.do", method = RequestMethod.POST)
	public void orderPayRefund(HttpServletRequest request, HttpServletResponse response, String tradeno, String orderno,
			String callback) {
		// LOG.info("[/pay/refund]");
		if (StringUtil.isEmpty(tradeno) && StringUtil.isEmpty(orderno)) {
			WebUtil.response(response, WebUtil.packJsonp(callback, JSON
					.toJSONString(new JsonResult(-1, "订单号不能为空", new ResponseData()), SerializerFeatureUtil.FEATURES)));
		}

		AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest(); // 统一收单交易退款接口
		// 只需要传入业务参数
		Map<String, Object> param = new HashMap<>();
		param.put("out_trade_no", orderno); // 商户订单号
		param.put("trade_no", tradeno);// 交易金额
		param.put("refund_amount", 0.01);// 退款金额
		param.put("refund_reason", "测试支付退款");// 退款金额
		param.put("out_request_no", PayUtil.getRefundNo()); // 退款单号
		alipayRequest.setBizContent(JSON.toJSONString(param)); // 不需要对json字符串转义
		Map<String, Object> restmap = new HashMap<>();// 返回支付宝退款信息
		boolean flag = false; // 查询状态
		try {
			AlipayTradeRefundResponse alipayResponse = AlipayConfig.getAlipayClient().execute(alipayRequest);
			if (alipayResponse.isSuccess()) {
				// 调用成功，则处理业务逻辑
				if ("10000".equals(alipayResponse.getCode())) {
					// 订单创建成功
					flag = true;
					restmap.put("out_trade_no", alipayResponse.getOutTradeNo());
					restmap.put("trade_no", alipayResponse.getTradeNo());
					restmap.put("buyer_logon_id", alipayResponse.getBuyerLogonId());// 用户的登录id
					restmap.put("gmt_refund_pay", alipayResponse.getGmtRefundPay()); // 退看支付时间
					restmap.put("buyer_user_id", alipayResponse.getBuyerUserId());// 买家在支付宝的用户id
					// LOG.info("订单退款结果：退款成功");
				} else {
					// LOG.info("订单查询失败：" + alipayResponse.getMsg() + ":" +
					// alipayResponse.getSubMsg());
				}
			}
		} catch (AlipayApiException e) {
			e.printStackTrace();
		}

		if (flag) {
			// 订单查询成功
			WebUtil.response(response,
					WebUtil.packJsonp(callback,
							JSON.toJSONString(new JsonResult(1, "订单退款成功", new ResponseData(null, restmap)),
									SerializerFeatureUtil.FEATURES)));
		} else { // 订单查询失败
			WebUtil.response(response, WebUtil.packJsonp(callback, JSON
					.toJSONString(new JsonResult(-1, "订单退款失败", new ResponseData()), SerializerFeatureUtil.FEATURES)));
		}
	}

	/**
	 *
	 * @param request
	 * @param response
	 * @param orderno
	 *            商家订单号
	 * @param tradeno
	 *            支付宝订单号
	 * @param callback
	 */
	@RequestMapping(value = "/refund/query.do", method = RequestMethod.POST)
	public void orderPayRefundQuery(HttpServletRequest request, HttpServletResponse response, String orderno,
			String tradeno, String callback) {

		if (StringUtil.isEmpty(orderno) && StringUtil.isEmpty(tradeno)) {
			WebUtil.response(response,
					WebUtil.packJsonp(callback,
							JSON.toJSONString(new JsonResult(-1, "商家订单号或支付宝订单号不能为空", new ResponseData()),
									SerializerFeatureUtil.FEATURES)));
		}

		AlipayTradeFastpayRefundQueryRequest alipayRequest = new AlipayTradeFastpayRefundQueryRequest(); // 统一收单交易退款查询
		// 只需要传入业务参数
		Map<String, Object> param = new HashMap<>();
		param.put("out_trade_no", orderno); // 商户订单号
		param.put("trade_no", tradeno);// 交易金额
		param.put("out_request_no", orderno);// 请求退款接口时，传入的退款请求号，如果在退款请求时未传入，则该值为创建交易时的外部交易号
		alipayRequest.setBizContent(JSON.toJSONString(param)); // 不需要对json字符串转义

		Map<String, Object> restmap = new HashMap<>();// 返回支付宝退款信息
		boolean flag = false; // 查询状态
		try {
			AlipayTradeFastpayRefundQueryResponse alipayResponse = AlipayConfig.getAlipayClient()
					.execute(alipayRequest);
			if (alipayResponse.isSuccess()) {
				// 调用成功，则处理业务逻辑
				if ("10000".equals(alipayResponse.getCode())) {
					// 订单创建成功
					flag = true;
					restmap.put("out_trade_no", alipayResponse.getOutTradeNo());
					restmap.put("trade_no", alipayResponse.getTradeNo());
					restmap.put("out_request_no", alipayResponse.getOutRequestNo());// 退款订单号
					restmap.put("refund_reason", alipayResponse.getRefundReason()); // 退款原因
					restmap.put("total_amount", alipayResponse.getTotalAmount());// 订单交易金额
					restmap.put("refund_amount", alipayResponse.getTotalAmount());// 订单退款金额
					// LOG.info("订单退款结果：退款成功");
				} else {
					// LOG.info("订单失败：" + alipayResponse.getMsg() + ":" +
					// alipayResponse.getSubMsg());
				}
			}
		} catch (AlipayApiException e) {
			e.printStackTrace();
		}

		if (flag) {
			// 订单查询成功
			WebUtil.response(response,
					WebUtil.packJsonp(callback,
							JSON.toJSONString(new JsonResult(1, "订单退款成功", new ResponseData(null, restmap)),
									SerializerFeatureUtil.FEATURES)));
		} else { // 订单查询失败
			WebUtil.response(response, WebUtil.packJsonp(callback, JSON
					.toJSONString(new JsonResult(-1, "订单退款失败", new ResponseData()), SerializerFeatureUtil.FEATURES)));
		}
	}

}
