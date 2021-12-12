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
		// 指定配置文件获取Spring Application Context容器
		ClassPathXmlApplicationContext ioc = new ClassPathXmlApplicationContext("spring-context-debug.xml");
		// 从ioc容器中获取注册的Bean实例对象
		TestBean testBean = ioc.getBean("testBean", TestBean.class);
		// 调用方法
		testBean.say();
	}

}
