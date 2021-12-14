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

	/**
	 * IOC 容器和对象创建的流程
	 * 1.先创建 IOC 容器
	 * 2.加载配置类, 封装成 BeanDefinition(Bean 定义信息)对象
	 * 3.调用执行 BeanFactoryPostProcessor
	 * 准备工作 :
	 * 准备 beanPostProcessor
	 * 准备监听器、事件、广播器
	 * 4.实例化
	 * 5.初始化
	 * 6.获取到完整的对象
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		// 指定配置文件获取Spring Application Context容器
		ClassPathXmlApplicationContext ioc = new ClassPathXmlApplicationContext("spring-context-debug.xml");
		// 从ioc容器中获取注册的Bean实例对象
		TestBean testBean = ioc.getBean("testBean", TestBean.class);
		// 调用方法
		testBean.say();
	}

}
