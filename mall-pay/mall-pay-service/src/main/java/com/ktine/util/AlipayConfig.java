package com.ktine.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConstants;
import com.alipay.api.DefaultAlipayClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AlipayConfig {

    private static Logger logger = LoggerFactory.getLogger(AlipayConfig.class);

    private static Properties properties = new Properties();

    static {
        InputStream inStream = null;
        try {
            String propFile;
            String resPath = AlipayConfig.class.getResource("/").getPath();
            if (resPath.endsWith("/"))
                propFile = resPath + "resources/alipay.properties";
            else
                propFile = resPath + "/resources/alipay.properties";

            inStream = new FileInputStream(new File(propFile));
            System.out.println(propFile);
            properties.load(inStream);

            if (logger.isInfoEnabled())
                logger.info("从属性文件 [" + propFile + "] 加载PC网页支付宝支付配置参数成功");
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
     * 获取支付宝接口版本号
     * @return 版本号
     */
    public static String getVersion() {
        return properties.getProperty("version");
    }

    /**
     * 获取合作伙伴id
     * @return 合作伙伴ids
     */
    public static String getPartnerId() {
        return properties.getProperty("partnerId");
    }

    /**
     * 获取卖家支付宝账号
     * @return 卖家支付宝账号
     */
    public static String getSellerAccountName() {
        return properties.getProperty("seller_account_name");
    }

    /**
     * 获取支付成功后跳转回本系统的页面地址
     * @return 支付成功后跳转回本系统的页面地址
     */
    public static String getCallBackUrl() {
        return properties.getProperty("call_back_url");
    }

    /**
     * 获取服务器异步通知地址
     * @return 服务器异步通知地址
     */
    public static String getNotifyUrl() {
        return properties.getProperty("notify_url");
    }

    /**
     * 获取用户付款中途退出返回商户的地址
     * @return 用户付款中途退出返回商户的地址
     */
    public static String getMerchantUrl() {
        return properties.getProperty("merchant_url");
    }

    /**
     * 获取交易自动关闭时间
     * @return 交易自动关闭时间
     */
    public static String getPayExpire() {
        return properties.getProperty("pay_expire");
    }

    /**
     * 获取请求参数格式
     * @return 请求参数格式
     */
    public static String getFormat() {
        return properties.getProperty("format");
    }

    /**
     * 获取签名方式 0001代表RSA签名算法；MD5代表MD5签名算法
     * @return 签名方式
     */
    public static String getSignType() {
        return properties.getProperty("sign_type");
    }

    /**
     * 获取支付宝安全校验码(账户内提取)，当sign_type=MD5时，该参数设置才有效
     * @return 支付宝安全校验码
     */
    public static String getKey() {
        return properties.getProperty("key");
    }

    /**
     * 获取商户方的私钥。当sign_type设置为 0001 时，该参数设置才有效。
     * @return 商户方的私钥
     */
    public static String getPrivateKey() {
        return properties.getProperty("private_key");
    }

    /**
     * 获取支付宝的公钥。当sign_type设置为 0001 时，该参数设置才有效。
     * @return 支付宝的公钥
     */
    public static String getPublicKey() {
        return properties.getProperty("public_key");
    }

    /**
     * 字符集编码，手机支付仅支持utf-8，不可修改
     * @return 字符集编码
     */
    public static String getInputCharset() {
        return properties.getProperty("input_charset");
    }

    /**
     * 获取授权接口名称
     * @return 授权接口名称
     */
    public static String getAuthService() {
        return properties.getProperty("auth_service");
    }
    /**
     * 获取交易接口名称
     * @return 交易接口名称
     */
    public static String getPayService() {
        return properties.getProperty("pay_service");
    }
    /**
     * 获取支付宝接口地址
     * @return 支付宝接口地址
     */
    public static String getAlipayUrl() {
        return properties.getProperty("alipay_url");
    }

    /**
     * 获取支付宝通知验证接口地址，用于验证是否是支付宝发送的通知，防钓鱼网站
     * @return 支付宝通知验证接口地址
     */
    public static String getVerifyUrl() {
        return properties.getProperty("verify_url");
    }


    /**
     * 获取接口日志保存路径
     * @return 接口日志保存路径
     */
    public static String getLogPath() {
        return properties.getProperty("log_path");
    }

    /**
     * 获取支付宝支付回调接口
     * @return 支付回调接口
     */
    public static String getPayCallbackUrl() { return properties.getProperty("ali_callback_url"); }
    
    /**
     * 统一收单交易创建接口
     */
 	private static AlipayClient alipayClient = null;

 	public static AlipayClient getAlipayClient() {
 		if (alipayClient == null) {
 			synchronized (AlipayConfig.class) {
 				if (null == alipayClient) {
 					alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", getPartnerId(),
 							getPrivateKey(), AlipayConstants.FORMAT_JSON, AlipayConstants.CHARSET_UTF8,
 							getPublicKey());
 				}
 			}
 		}
 		return alipayClient;
 	}
}

