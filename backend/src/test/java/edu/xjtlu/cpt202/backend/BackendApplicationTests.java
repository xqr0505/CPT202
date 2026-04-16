package edu.xjtlu.cpt202.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
/**
 * @author QiranXiao
 * @date 2026/4/1
 *
 */
@SpringBootTest(properties = {
		"ai.openai.api-key=test-key",
		"ai.openai.model-name=test-model"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
