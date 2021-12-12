package com.kapcb.ccc;

import com.kapcb.ccc.components.TestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * <a>Title: MainApplication </a>
 * <a>Author: Kapcb <a>
 * <a>Description: MainApplication <a>
 *
 * @author Kapcb
 * @version 1.0
 * @date 2021/12/11 23:19
 * @since 1.0
 */
public class MainApplication {

	public static void main(String[] args) {
		ClassPathXmlApplicationContext ioc = new ClassPathXmlApplicationContext("spring-context-debug.xml");
		TestBean testBean = ioc.getBean("testBean", TestBean.class);
		testBean.say();
	}

}
