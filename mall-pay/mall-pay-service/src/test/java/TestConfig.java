import org.junit.Test;

import com.ktine.util.WeixinConfig;

public class TestConfig {
	
	@Test
	public  void testConfig() {
		System.out.println("hello world");
		System.out.println(WeixinConfig.getApibKey());
	}
}
