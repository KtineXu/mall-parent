1. 实现支付的dao层服务操作实现（PayService接口）
2. 修改weixinpay.properties，alipay.properties配置文件信息
3. 安装支付宝支付SDK，将alipay-sdk-java-3.0.0.jarjar至本地:

		mvn install:install-file -DgroupId=alipay -DartifactId=sdk-java -Dversion=3.0.0 -Dpackaging=jar -Dfile=alipay-sdk-java-3.0.0.jar 

		<dependency>  
		    <groupId>alipay</groupId>  
		    <artifactId>sdk-java</artifactId>  
		    <version>3.0.0</version>  
		</dependency> 