package com.ktine.service;

public interface PayService {

	/**
	 * 用户充值前记录充值细节
	 * @param orderId 订单号
	 * @param userId 用户ID
	 * @param type 充值方式weixinPay 、aliPay 、Bank
	 * @param price 充值金额
	 */
	void savePreDetail(String orderId, String userId, String type, Integer price);

	/**
	 * 判断支付是否成功的接口。如果支付成功，会更新资金账户表，修改充值明细表
	 * @param outTradeNo
	 * @param price
	 * @return
	 */
	boolean updateDepositDetail(String outTradeNo, int price);
	
}
