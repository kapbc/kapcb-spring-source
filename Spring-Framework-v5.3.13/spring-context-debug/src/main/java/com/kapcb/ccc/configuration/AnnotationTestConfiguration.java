package com.kapcb.ccc.configuration;

import com.kapcb.ccc.model.DebugBean;
import com.kapcb.ccc.model.TestBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * <a>Title: AnnotationTestConfiguration </a>
 * <a>Author: Mike Chen <a>
 * <a>Description: AnnotationTestConfiguration <a>
 *
 * @author Mike Chen
 * @date 2021/12/15 16:06
 */
@Configuration
public class AnnotationTestConfiguration {

	@Bean("testBean")
	@Scope("singleton")
	public DebugBean debugBean() {
		TestBean testBean = new TestBean();
		testBean.setUsername("Kapcb(Annotation)");
		return testBean;
	}

}
