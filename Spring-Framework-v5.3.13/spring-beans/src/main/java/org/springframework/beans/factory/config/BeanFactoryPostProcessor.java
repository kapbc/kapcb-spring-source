/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;

/**
 * Factory hook that allows for custom modification of an application context's
 * bean definitions, adapting the bean property values of the context's underlying
 * bean factory.
 *
 * <p>Useful for custom config files targeted at system administrators that
 * override bean properties configured in the application context. See
 * {@link PropertyResourceConfigurer} and its concrete implementations for
 * out-of-the-box solutions that address such configuration needs.
 *
 * <p>A {@code BeanFactoryPostProcessor} may interact with and modify bean
 * definitions, but never bean instances. Doing so may cause premature bean
 * instantiation, violating the container and causing unintended side-effects.
 * If bean instance interaction is required, consider implementing
 * {@link BeanPostProcessor} instead.
 *
 * <h3>Registration</h3>
 * <p>An {@code ApplicationContext} auto-detects {@code BeanFactoryPostProcessor}
 * beans in its bean definitions and applies them before any other beans get created.
 * A {@code BeanFactoryPostProcessor} may also be registered programmatically
 * with a {@code ConfigurableApplicationContext}.
 *
 * <h3>Ordering</h3>
 * <p>{@code BeanFactoryPostProcessor} beans that are autodetected in an
 * {@code ApplicationContext} will be ordered according to
 * {@link org.springframework.core.PriorityOrdered} and
 * {@link org.springframework.core.Ordered} semantics. In contrast,
 * {@code BeanFactoryPostProcessor} beans that are registered programmatically
 * with a {@code ConfigurableApplicationContext} will be applied in the order of
 * registration; any ordering semantics expressed through implementing the
 * {@code PriorityOrdered} or {@code Ordered} interface will be ignored for
 * programmatically registered post-processors. Furthermore, the
 * {@link org.springframework.core.annotation.Order @Order} annotation is not
 * taken into account for {@code BeanFactoryPostProcessor} beans.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 06.07.2003
 * @see BeanPostProcessor
 * @see PropertyResourceConfigurer
 */
@FunctionalInterface
public interface BeanFactoryPostProcessor {

	/**
	 * BeanFactoryPostProcessor 相较于 BeanPostProcessor 方法是很简单的, 只有一个方法
	 * 其子接口 BeanDefinitionRegistryPostProcessor 也只有一个方法。但是它们的功能相似,
	 * 区别在于作用域不同。
	 *
	 * BeanFactoryPostProcessor 作用域范围是容器级别的。它只与使用的容器相关。如果在容器中
	 * 定义一个 Bean 的 BeanFactoryPostProcessor, 它仅仅只对此容器中的 Bean 进行后置增强
	 * 处理
	 *
	 * BeanFactoryPostProcessor 不会对定义在另一个容器中的 Bean 进行后置增强处理, 即使这
	 * 两个容器都在同一个容器中。
	 *
	 * BeanFactoryPostProcessor 可以对 Bean 的定义信息(配置元数据 BeanDefinition)进行
	 * 处理。Spring IOC 容器允许 BeanFactoryPostProcessor 在容器实际实例化任何其他 Bean
	 * 之前读取 Bean 的配置元数据, 并可以修改它。简而言之就是 BeanFactoryPostProcessor 接
	 * 口是直接修改了 Bean 的定义信息, 而 BeanPostProcessor 则是对 Bean 创建过程中进行增强
	 * 操作。
	 *
	 * BeanDefinitionRegistryPostProcessor 和 BeanFactoryPostProcessor 的区别在于 :
	 * 1、BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry 方法
	 * 针对的是 BeanDefinitionRegistry 类型的 ConfigurableListableBeanFactory, 可以
	 * 实现对 BeanDefinition 的增删改查操作, 但是对非 ConfigurableListableBeanFactory 类型
	 * 的 BeanFactory 是不起作用的。
	 *
	 * 2、BeanFactoryPostProcessor#postProcessBeanFactory 方法针对的是所有类型的 BeanFactory
	 *
	 * 3、postProcessBeanDefinitionRegistry 方法的调用时机在 postProcessBeanFactory 之前
	 *
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for overriding or adding
	 * properties even to eager-initializing beans.
	 * @param beanFactory the bean factory used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;

}
