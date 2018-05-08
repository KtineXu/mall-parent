package com.ktine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class WeixinConfig {

	private static Logger logger = LoggerFactory.getLogger(WeixinConfig.class);

	private static Properties properties = new Properties();

	static {
		InputStream inStream = null;
		try {
			String propFile;
			String resPath = WeixinConfig.class.getResource("/").getPath();
			System.out.println("--------->"+resPath+"<------------------");
			if (resPath.endsWith("/"))
				propFile = resPath + "resources/weixinpay.properties";
			else
				propFile = resPath + "/resources/weixinpay.properties";

			inStream = new FileInputStream(new File(propFile));
			properties.load(inStream);

			if (logger.isInfoEnabled())
				logger.info("从属性文件 [" + propFile + "] 加载PC网页微信支付配置参数成功");
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (inStream != null)
					inStream.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}
	}
	/**
	 * 
	 */
	public static String  getTradeType() {
		return properties.getProperty("trade_type");
	}
	/**
	 * 获取token接口(GET)
	 * @return
	 */
	public static String getTokenUrl(){
		return properties.getProperty("token_url");
	}

	/**
	 * 获取微信服务号
	 * @return 服务号
	 */
	public static String getAppId() {
		return properties.getProperty("appid");
	}

	/**
	 * 获取微信应用密码
	 * @return 应用密码
	 */
	public static String getAppSecrect() {
		return properties.getProperty("app_secrect");
	}

	/**
	 * 获取微信服务号的配置token
	 * @return token
	 */
	public static String getToken() {
		return properties.getProperty("token");
	}

	/**
	 * 获取微信商户号
	 * @return 商户号
	 */
	public static String getMchId() {
		return properties.getProperty("mch_id");
	}

	/**
	 * 获取微信API密钥
	 * @return API密钥
	 */
	public static String getApiKey() {
		return properties.getProperty("api_key");
	}

	/**
	 * 获取微信签名加密方式
	 * @return 签名加密方式
	 */
	public static String getSignType() {
		return properties.getProperty("sign_type");
	}

	/**
	 * 获取微信签名加密方式
	 * @return 签名加密方式
	 */
	public static String getUnifiedOrderUrl() {
		return properties.getProperty("unified_order_url");
	}

	/**
	 * 获取微信签名加密方式
	 * @return 签名加密方式
	 */
	public static String getCheckOrderUrl() {
		return properties.getProperty("check_order_url");
	}

	/**
	 * 获取微信支付回调接口
	 * @return 支付回调接口
     */
	public static String getPayCallbackUrl() { return properties.getProperty("weixin_callback_url"); }
	
	//-----------------------手机版c端微信信息----------------------------
	/**
	 * 获取微信服务号
	 * @return 服务号
	 */
	public static String getAppcId() {
		return properties.getProperty("appcid");
	}
	/**
	 * 
	 */
	public static String getKey(){
		return properties.getProperty("key");
	}
	
	/**
	 * 获取微信商户号
	 * @return 商户号
	 */
	public static String getMchcId() {
		return properties.getProperty("mchc_id");
	}
	
	/**
	 * 获取微信API密钥
	 * @return API密钥
	 */
	public static String getApicKey() {
		return properties.getProperty("c_key");
	}
	//--------------------------手机版c端微信信息--------------------------
	
	
	//-----------------------手机版b端微信信息----------------------------
		/**
		 * 获取微信服务号
		 * @return 服务号
		 */
		public static String getAppbId() {
			return properties.getProperty("appbid");
		}
		
		/**
		 * 获取微信商户号
		 * @return 商户号
		 */
		public static String getMchbId() {
			return properties.getProperty("mchb_id");
		}
		
		/**
		 * 获取微信API密钥
		 * @return API密钥
		 */
		public static String getApibKey() {
			return properties.getProperty("b_key");
		}
		//--------------------------手机版b端微信信息--------------------------

}
