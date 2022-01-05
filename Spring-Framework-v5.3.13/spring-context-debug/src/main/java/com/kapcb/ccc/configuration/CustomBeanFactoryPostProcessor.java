package com.kapcb.ccc.configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * <a>Title: CustomBeanFactoryPostProcessor </a>
 * <a>Author: Kapcb <a>
 * <a>Description: CustomBeanFactoryPostProcessor <a>
 *
 * @author Kapcb
 * @version 1.0
 * @date 2022/1/5 23:32
 * @since 1.0
 */
public class CustomBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	/**
	 * 可以在 Bean 实例化之前对指定 Bean 做任何定制化操作  --  为所欲为
	 *
	 * @param beanFactory the bean factory used by the application context
	 * @throws BeansException BeansException
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinition testBeanBeanDefinition = beanFactory.getBeanDefinition("testBean");
		testBeanBeanDefinition.setScope("singleton");
	}

}
