package com.kapcb.ccc;

import com.kapcb.ccc.configuration.AnnotationTestConfiguration;
import com.kapcb.ccc.model.DebugBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * <a>Title: AnnotationApplication </a>
 * <a>Author: Mike Chen <a>
 * <a>Description: AnnotationApplication <a>
 *
 * @author Mike Chen
 * @date 2021/12/15 16:05
 */
public class AnnotationApplication {

	public static void main(String[] args) {

		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(AnnotationTestConfiguration.class);
		DebugBean testBean = applicationContext.getBean("testBean", DebugBean.class);
		testBean.say();

	}
}
