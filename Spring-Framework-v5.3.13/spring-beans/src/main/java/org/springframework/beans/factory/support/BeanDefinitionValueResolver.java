/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Helper class for use in bean factory implementations,
 * resolving values contained in bean definition objects
 * into the actual values applied to the target bean instance.
 *
 * <p>Operates on an {@link AbstractBeanFactory} and a plain
 * {@link org.springframework.beans.factory.config.BeanDefinition} object.
 * Used by {@link AbstractAutowireCapableBeanFactory}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see AbstractAutowireCapableBeanFactory
 *
 * 在 Bean 工厂中实现中使用 Helper 类, 它将 BeanDefinition 对象中包含的值解析为应用于
 * 目标 Bean 实例的实际值
 *
 * 在 AbastractBeanFactory 和纯 BeanDefinition 对象上操作, 由 AbstractAutowireCapabaleBeanFactory 使用
 */
class BeanDefinitionValueResolver {

	// 当前 Bean 工厂
	private final AbstractAutowireCapableBeanFactory beanFactory;

	// 要使用的 beanName
	private final String beanName;

	// beanName 对应的 BeanDefinition
	private final BeanDefinition beanDefinition;

	// 用于解析 TypeStringValues 的 TypeConverter
	private final TypeConverter typeConverter;


	/**
	 * Create a BeanDefinitionValueResolver for the given BeanFactory and BeanDefinition.
	 * @param beanFactory the BeanFactory to resolve against -- 要解决的 BeanFactory, 即当前 BeanFactory
	 * @param beanName the name of the bean that we work on -- 要使用的 BeanName
	 * @param beanDefinition the BeanDefinition of the bean that we work on -- 要使用的 Bean 的 BeanDefinition
	 * @param typeConverter the TypeConverter to use for resolving TypedStringValues -- 用于解析 TypeStringValues 的 TypeConverter
	 *
	 * 为给定的 BeanFactory 和 BeanDefinition 创建一个 BeanDefinitionValueResolver 实例
	 */
	public BeanDefinitionValueResolver(AbstractAutowireCapableBeanFactory beanFactory, String beanName,
			BeanDefinition beanDefinition, TypeConverter typeConverter) {

		this.beanFactory = beanFactory;
		this.beanName = beanName;
		this.beanDefinition = beanDefinition;
		this.typeConverter = typeConverter;
	}


	/**
	 * Given a PropertyValue, return a value, resolving any references to other
	 * beans in the factory if necessary. The value could be:
	 * <li>A BeanDefinition, which leads to the creation of a corresponding
	 * new bean instance. Singleton flags and names of such "inner beans"
	 * are always ignored: Inner beans are anonymous prototypes.
	 * <li>A RuntimeBeanReference, which must be resolved.
	 * <li>A ManagedList. This is a special collection that may contain
	 * RuntimeBeanReferences or Collections that will need to be resolved.
	 * <li>A ManagedSet. May also contain RuntimeBeanReferences or
	 * Collections that will need to be resolved.
	 * <li>A ManagedMap. In this case the value may be a RuntimeBeanReference
	 * or Collection that will need to be resolved.
	 * <li>An ordinary object or {@code null}, in which case it's left alone.
	 * @param argName the name of the argument that the value is defined for -- 为参数定义的参数名
	 * @param value the value object to resolve -- 要解决的对象值
	 * @return the resolved object -- 解析的对象
	 *
	 * 给定一个 PropertyValue, 返回一个值, 如果有必要, 解析对工厂中其它 Bean 的任何引用, 该值可以是 :
	 * 1. BeanDefinition : 它会创建相应的新 Bean 实例。此类 '内部 Bean' 的单例标志和名称始终被忽略 : 内部 Bean 是匿名原型
	 * 2. RuntimeBeanReference : 运行时 Bean 的引用, 必须解决
	 * 3. ManagedList : 这是一个特殊集合, 其中可能包含需要解决的 RuntimeBeanReferences 或 Collections
	 * 4. ManagedSet : 可能包含需要解决的 RuntimeBeanReferences 或 Collections
	 * 5. ManagedMap : 在这种情况下, 该值可能是需要解析的 RuntimeBeanReferences 或 Collections
	 * 6. 普通对象或 null, 在这种情况下将其保留
	 */
	@Nullable
	public Object resolveValueIfNecessary(Object argName, @Nullable Object value) {
		// We must check each value to see whether it requires a runtime reference
		// to another bean to be resolved.
		// 我们必须检查每一个值, 以查看它是否需要对另一个 Bean 的运行时引用才能解决
		// RuntimeBeanReference : 当属性值对象是工厂中另外一个 Bean 的引用时, 使用不可变的占位符类, 在运行时进行解析
		// 在下面这种配置就会生成 RuntimeBeanReference :
		// <bean class="com.kapcb.ccc.TestBean">
		// 		<property name="referBeanName" ref="otherBeanName" />
		// </bean>

		// 如果 value 是 RuntimeBeanReference 实例
		if (value instanceof RuntimeBeanReference) {
			// 将 value 强制转换为 RuntimeBeanReference 对象
			RuntimeBeanReference ref = (RuntimeBeanReference) value;
			// 解析出对应 ref 所封装的 Bean 元信息(即 BeanName, Bean 类型)的 Bean 对象
			return resolveReference(argName, ref);
		}
		// RuntimeBeanNameReference 对应于 -> <idref bean="kapcb" />
		// idref 注入的目标是 bean 的 id 而不是目标 bean 实例, 同时使用 idref 容器在部署的时候还会验证这个名称的 bean
		// 是否真是存在。其实 idref 就跟 value 一样, 只是将某个字符串注入到属性或者构造函数中, 只不过注入的是某个 bean 定义
		// 的 id 属性值 :
		// 即 : <idref bean="kapcb" /> 等同于 <value>kapcb</value>
		// 如果 value 是 RuntimeBeanNameReference
		else if (value instanceof RuntimeBeanNameReference) {
			// 从 value 中获取引用的 beanName
			String refName = ((RuntimeBeanNameReference) value).getBeanName();
			// 对 refName 进行解析, 然后重新赋值给 refName
			refName = String.valueOf(doEvaluate(refName));
			// 如果该 bean 工厂不包含具有 refName 的 BeanDefinition 或外部注册的 Singleton 实例
			if (!this.beanFactory.containsBean(refName)) {
				// 抛出 BeanDefinitionStoreException 异常 : argName 的 Bean 引用中的 Bean 名 'refName' 无效
				throw new BeanDefinitionStoreException(
						"Invalid bean name '" + refName + "' in bean reference for " + argName);
			}
			// 返回经过解析且经过检查其是否存在与 Bean 工厂的引用 BeanName[refName]
			return refName;
		}
		// BeanDefinitionHolder : 据由名称和别名的 Bean 定义的持有者, 可以注册为内部 Bean 的占位符
		// 如果出现 BeanDefinitionHolder 的情况, 一般是内部 Bean 配置
		// 如果 value 是 BeanDefinitionHolder 实例
		else if (value instanceof BeanDefinitionHolder) {
			// Resolve BeanDefinitionHolder: contains BeanDefinition with name and aliases.
			// 解决 BeanDefinitionHolder : 包含具有名称和别名的 BeanDefinition
			// 将 value 强制转换为 BeanDefinitionHolder 对象
			BeanDefinitionHolder bdHolder = (BeanDefinitionHolder) value;
			// 根据 BeanDefinitionHolder 所封装的 BeanName 和 BeanDefinition 对象解析出内部 Bean 对象
			return resolveInnerBean(argName, bdHolder.getBeanName(), bdHolder.getBeanDefinition());
		}
		// 一般在内部匿名 Bean 的配置才会出现 BeanDefinition, 如下 :
		// <bean id="kapcb" class="com.kapcb.ccc.model.Person">
		// 	 <property name="name" value="kapcb"/>
		// 	 <property name="age" value="18"/>
		//	 <property name="school">
		//		 <bean class="com.kapcb.ccc.model.School">
		//			 <property name="schoolName" value="PanLongErXiao"/>
		//			 <property name="location" value="湖北省武汉市"/>
		//		 </bean>
		//	 </property>
		// </bean>
		// 如果 value 是 BeanDefinition 实例
		else if (value instanceof BeanDefinition) {
			// Resolve plain BeanDefinition, without contained name: use dummy name.
			// 解析纯 BeanDefinition, 不包含名称 : 使用虚拟名称
			// 将 value 强制类型转换为 BeanDefinition 对象
			BeanDefinition bd = (BeanDefinition) value;
			// 拼装内部 bean 名称 : "(inner bean)#" + BeanDefinition 的身份哈希码的十六进制字符串形式
			String innerBeanName = "(inner bean)" + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR +
					ObjectUtils.getIdentityHexString(bd);
			// 根据拼装的 innerBeanName 和 BeanDefinition 解析出内部 Bean 对象
			return resolveInnerBean(argName, innerBeanName, bd);
		}
		// 如果 value 是 DependencyDescriptor 实例
		else if (value instanceof DependencyDescriptor) {
			// 定义一个用于存放找到的所有候选 BeanName 的集合, 初始化长度为4
			Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
			// 根据 descriptor 的依赖类型解析出与 descriptor 所包装的对象匹配的候选 Bean 对象
			Object result = this.beanFactory.resolveDependency(
					(DependencyDescriptor) value, this.beanName, autowiredBeanNames, this.typeConverter);
			// 遍历 autowiredBeanNames
			for (String autowiredBeanName : autowiredBeanNames) {
				// 如果该 BeanFactory 包含具有 autowiredBeanName 的 BeanDefinition 或外部注册的 Singleton 实例
				if (this.beanFactory.containsBean(autowiredBeanName)) {
					// 注册 autowiredBeanName 与 beanName 的依赖关系
					this.beanFactory.registerDependentBean(autowiredBeanName, this.beanName);
				}
			}
			// 返回与 descriptor 所包装的对象匹配的候选 Bean 对象
			return result;
		}

		// 一般在 <array></array> 标签才会出现 ManagedArray, 如下 :
		// <property name="girlFriends">
		//	 <array>
		//		 <value>girl friend one</value>
		//		 <value>girl friend two</value>
		//		 <value>girl friend three</value>
		//		 <value>girl friend four</value>
		//	 </array>
		// </property>
		// 如果 value 是 ManagedArray 实例
		else if (value instanceof ManagedArray) {
			// May need to resolve contained runtime references.
			// 可能需要解析包含的运行时引用
			// 将 value 类型强制转换为 ManagedArray 对象
			ManagedArray array = (ManagedArray) value;
			// 获取 array 的已解析元素类型
			Class<?> elementType = array.resolvedElementType;
			// 如果 elementType 为 null
			if (elementType == null) {
				// 获取 array 的元素类型名称, 指 array 标签的 value-type 属性
				String elementTypeName = array.getElementTypeName();
				// 如果 elementTypeName 不是空字符串
				if (StringUtils.hasText(elementTypeName)) {
					try {
						// 使用 BeanFactory 工厂的 Bean 类型加载器加载 elementTypeName 对应的 class 对象
						elementType = ClassUtils.forName(elementTypeName, this.beanFactory.getBeanClassLoader());
						// 让 array#resolvedElementType 属性引用 elementType
						array.resolvedElementType = elementType;
					}
					// 捕捉加载 elementTypeName 对应的 Class 对象的所有异常
					catch (Throwable ex) {
						// Improve the message by showing the context.
						// 通过显示上下文来改善消息
						// 抛出 BeanCreationException 异常 : 错误解析数组类型参数
						throw new BeanCreationException(
								this.beanDefinition.getResourceDescription(), this.beanName,
								"Error resolving array type for " + argName, ex);
					}
				}
				else {
					// 让 elementTypeName 默认使用 Object 对象
					elementType = Object.class;
				}
			}
			// 解析 ManagedArray 对象, 以得到解析后的数组对象
			return resolveManagedArray(argName, (List<?>) value, elementType);
		}

		// 一般在 <list></list> 标签才会出现 ManagedList :
		// <property name="boyFriends">
		//	 <list>
		//		 <value>boy friend one</value>
		//		 <value>boy friend two</value>
		//		 <value>boy friend three</value>
		//		 <value>boy friend four</value>
		//	 </list>
		// </property>
		// 如果 value 是 ManagedList 实例
		else if (value instanceof ManagedList) {
			// May need to resolve contained runtime references.
			// 可能需要解析包含的运行时引用
			// 解析 ManagedList 对象, 以得到解析后的 List 对象并将结果返回出去
			return resolveManagedList(argName, (List<?>) value);
		}

		// 一般在 <set></set> 标签才会出现 ManagedSet
		// <property name="phoneNumbers">
		//	 <set>
		//		 <value>123456789</value>
		//		 <value>987654321</value>
		//		 <value>543216789</value>
		//		 <value>666688888</value>
		//	 </set>
		// </property>
		// 如果 value 是 ManagedSet 对象
		else if (value instanceof ManagedSet) {
			// May need to resolve contained runtime references.
			// 可能需要解析包含的运行时引用
			// 解析 ManagedSet 对象, 以得到解析后的 Set 对象并将结果返回出去
			return resolveManagedSet(argName, (Set<?>) value);
		}

		// 一般在 <map></map> 标签才会出现 ManagedMap
		// <property name="houses">
		//	 <map>
		//		 <entry key="one" value-ref="houseOne"/>
		//		 <entry key="two" value-ref="houseTwo"/>
		//	 </map>
		// </property>
		// 如果 value 是 ManagedMap 对象
		else if (value instanceof ManagedMap) {
			// May need to resolve contained runtime references.
			// 可能需要解析包含的运行时引用
			// 解析 ManagedMap 对象, 以得到解析后的 Map 对象并将结果返回出去
			return resolveManagedMap(argName, (Map<?, ?>) value);
		}

		// 一般 <props></props> 标签才会出现 ManagedProperties
		// <props>
		//	<prop key="setA*">PROPAGATION_REQUIRED</prop>
		//	<prop key="rollbackOnly">PROPAGATION_REQUIRED</prop>
		//	<prop key="echoException">PROPAGATION_REQUIRED, +javax.servlet.ServletException, -java.lang.Exception</prop>
		//	<prop key="setA*">PROPAGATION_REQUIRED</prop>
		// <props>
		// 如果 value 是 ManagedProperties 对象
		else if (value instanceof ManagedProperties) {
			// 将 value 强制类型转换为 Properties 对象
			Properties original = (Properties) value;
			// 定义一个用于存储 original 的所有 Property 的 键/值 解析后的 键/值 的 Properties 对象
			Properties copy = new Properties();
			// 遍历 original, 键名为 propKey, 值为 propValue
			original.forEach((propKey, propValue) -> {
				// 如果 proKey 是 TypedStringValue 实例
				if (propKey instanceof TypedStringValue) {
					// 在 propKey 封装的 value 可解析成表达式的情况下, 将 propKey 封装的 value 评估为表达式并解析出表达式的值
					propKey = evaluate((TypedStringValue) propKey);
				}
				// 如果 propValue 是 TypedStringValue 实例
				if (propValue instanceof TypedStringValue) {
					// 在 propValue 封装的 value 可解析成表达式的情况下, 将 propValue 封装的 value 评估为表达式并解析出表达式的值
					propValue = evaluate((TypedStringValue) propValue);
				}
				// 如果 propKey || propValue 为 null
				if (propKey == null || propValue == null) {
					// 抛出 BeanCreationException 异常 : 转换 argName 的属性 键/值 时出错 : 解析为 null
					throw new BeanCreationException(
							this.beanDefinition.getResourceDescription(), this.beanName,
							"Error converting Properties key/value pair for " + argName + ": resolved to null");
				}
				// 将 propKey 和 propValue 添加到 copy 中
				copy.put(propKey, propValue);
			});
			// 返回 copy
			return copy;
		}
		// 如果 value 为 TypedStringValue 实例
		else if (value instanceof TypedStringValue) {
			// Convert value to target type here.
			// 在此处将 value 转换为目标类型
			// 将 value 强制转换为 TypedStringValue 对象
			TypedStringValue typedStringValue = (TypedStringValue) value;
			// 在 TypedStringValue 封装的 value 可解析成表达式的情况下, 将 TypedStringValue 封装的 value 评估为表达式并解析出表达式的值
			Object valueObject = evaluate(typedStringValue);
			try {
				// 在 TypedStringValue 中解析目标类型
				Class<?> resolvedTargetType = resolveTargetType(typedStringValue);
				// 如果 resolvedTargetType 不为空
				if (resolvedTargetType != null) {
					// 使用 typeConverter 将值转换为所需的类型
					return this.typeConverter.convertIfNecessary(valueObject, resolvedTargetType);
				}
				else {
					// 返回解析出来表达式的值
					return valueObject;
				}
			}
			// 捕捉在解析目标类型或转换类型过程中抛出的异常
			catch (Throwable ex) {
				// Improve the message by showing the context.
				// 通过显示上下文来改善消息
				// 抛出 BeanCreationException 异常 : 为 argName 转换键入的字符串值时错误
				throw new BeanCreationException(
						this.beanDefinition.getResourceDescription(), this.beanName,
						"Error converting typed String value for " + argName, ex);
			}
		}
		// 如果 value 为 NullBean
		else if (value instanceof NullBean) {
			// 直接返回 null
			return null;
		}
		else {
			// 对于 value 是 String || String[] 类型会尝试评估为表达式并解析出表达式的值, 其它类型直接返回 value
			return evaluate(value);
		}
	}

	/**
	 * Evaluate the given value as an expression, if necessary.
	 * @param value the candidate value (may be an expression) -- 候选值(可以是表达式)
	 * @return the resolved value -- 解析值
	 *
	 * 在 value 封装的 value 可解析为表达式的情况下, 将 value 封装的 value 评估为表达式并解析出表达式的值
	 * 如果有必要(value 可解析成表达式的情况下), 将 value 封装的 value 评估为表达式
	 * 如果 result 与 value 所封装的 value 不相等, 将 value 标记为动态, 即包含一个表达式, 因此不进行缓存
	 */
	@Nullable
	protected Object evaluate(TypedStringValue value) {
		// 如果又必要(value 可解析成表达式的情况下), 将 value 封装的 value 评估为表达式并解析出表达式的值
		Object result = doEvaluate(value.getValue());
		// 如果 result 与 value 所封装的 value 不相等
		if (!ObjectUtils.nullSafeEquals(result, value.getValue())) {
			// 将 value 标记为动态, 即包含一个表达式, 因此不进行缓存
			value.setDynamic();
		}
		// 返回 result
		return result;
	}

	/**
	 * Evaluate the given value as an expression, if necessary.
	 * @param value the original value (may be an expression)
	 * @return the resolved value if necessary, or the original value
	 */
	@Nullable
	protected Object evaluate(@Nullable Object value) {
		// 如果 value 是 String 类型实例
		if (value instanceof String) {
			// 如果有必要(value 可解析成表达式的情况下), 将 value 评估为表达式并解析出表达式的值并返回出去
			return doEvaluate((String) value);
		}
		// 如果 value 是 String数组
		else if (value instanceof String[]) {
			// 将 value 强制类型转换为 String[]
			String[] values = (String[]) value;
			// 是否经过解析的标记, 默认为 false
			boolean actuallyResolved = false;
			// 定义用于存放解析的值的 Object 数组, 长度为 values 的长度
			Object[] resolvedValues = new Object[values.length];
			// 遍历 values
			for (int i = 0; i < values.length; i++) {
				// 获取第 i 个 values 的元素
				String originalValue = values[i];
				// 如果有必要(value 可解析成表达式的情况下), 将 originalValue 评估为表达式并解析出表达式的值
				Object resolvedValue = doEvaluate(originalValue);
				// 如果 resolvedValue 与 originalValue 不是同一个对象
				if (resolvedValue != originalValue) {
					// 经过解析标记为 true, 表示已经经过解析
					actuallyResolved = true;
				}
				// 将 resolvedValue 赋值给第 i 个 resolvedValues 元素
				resolvedValues[i] = resolvedValue;
			}
			// 如果已经解析过, 返回解析后的数组 resolvedValues, 否则返回 values
			return (actuallyResolved ? resolvedValues : values);
		}
		else {
			// 其它类型直接返回 value
			return value;
		}
	}

	/**
	 * Evaluate the given String value as an expression, if necessary.
	 * @param value the original value (may be an expression)  -- 原始值(可能是一个表达式)
	 * @return the resolved value if necessary, or the original String value -- 必要时解析的值或原始 String 值
	 *
	 * 如果有必要(value 可解析成表达式的情况瞎), 将给定的 String 值评估为表达式并解析出表达式的值
	 */
	@Nullable
	private Object doEvaluate(@Nullable String value) {
		// 评估 value, 如果 value 是可解析表达式, 会对其进行解析, 否则直接返回 value
		return this.beanFactory.evaluateBeanDefinitionString(value, this.beanDefinition);
	}

	/**
	 * Resolve the target type in the given TypedStringValue.
	 * @param value the TypedStringValue to resolve  -- 要解析的 TypeStringValue
	 * @return the resolved target type (or {@code null} if none specified)  -- 解析的目标(如果未指定, 则为 null)
	 * @throws ClassNotFoundException if the specified type cannot be resolved  -- 如果无法解析指定的类型
	 * @see TypedStringValue#resolveTargetType
	 *
	 * 在给定的 TypeStringValue 中解析目标类型
	 */
	@Nullable
	protected Class<?> resolveTargetType(TypedStringValue value) throws ClassNotFoundException {
		// 如果 value 有携带目标类型
		if (value.hasTargetType()) {
			// 返回 value 的目标类型
			return value.getTargetType();
		}
		// 从 value 中解析出目标类型
		return value.resolveTargetType(this.beanFactory.getBeanClassLoader());
	}

	/**
	 * Resolve a reference to another bean in the factory.
	 * @param argName 定义值的参数名
	 * @param ref 封装着另一个 bean 的引用的 RuntimeBeanReference 对象
	 * @return Object
	 *
	 * 在工厂中解决对另一个 bean 的引用
	 */
	@Nullable
	private Object resolveReference(Object argName, RuntimeBeanReference ref) {
		try {
			// 定义一个存储 Bean 对象的变量
			Object bean;
			// 获取另一个 Bean 引用的 Bean 的类型
			Class<?> beanType = ref.getBeanType();
			// 如果引用来自父工厂
			if (ref.isToParent()) {
				// 获取父工厂
				BeanFactory parent = this.beanFactory.getParentBeanFactory();
				// 如果没有父工厂
				if (parent == null) {
					// 抛出 BeanCreatingException : 无法解析对 bean 的引用, ref在父工厂中 : 没有可用的父工厂
					throw new BeanCreationException(
							this.beanDefinition.getResourceDescription(), this.beanName,
							"Cannot resolve reference to bean " + ref +
									" in parent factory: no parent factory available");
				}
				// 如果引用的 Bean 类型不为 null
				if (beanType != null) {
					// 从父工厂中获取引用的 Bean 类型对应的 Bean 实例对象
					bean = parent.getBean(beanType);
				}
				else {
					// 否则使用引用 Bean 的 BeanName, 从父工厂中获取对应的 Bean 实例对象
					bean = parent.getBean(String.valueOf(doEvaluate(ref.getBeanName())));
				}
			}
			else {
				// 定义一个用于存放解析出来的 beanName 的变量
				String resolvedName;
				// 如果 beanType 不为空
				if (beanType != null) {
					// 解析于 beanType 匹配的唯一 Bean 实例, 包括其 BeanName
					NamedBeanHolder<?> namedBean = this.beanFactory.resolveNamedBean(beanType);
					// 让 bean 引用 namedBean 所封装的 Bean 对象
					bean = namedBean.getBeanInstance();
					// 让 resolvedName 引用 namedBean 所封装的 BeanName
					resolvedName = namedBean.getBeanName();
				}
				else {
					// 让 resolvedName 引用 ref 所包装的 Bean 名
					resolvedName = String.valueOf(doEvaluate(ref.getBeanName()));
					// 获取 resolvedName 的 Bean 实例对象
					bean = this.beanFactory.getBean(resolvedName);
				}
				// 注册 beanName 与 dependentBeanNamed 的依赖关系到 Bean 工厂
				this.beanFactory.registerDependentBean(resolvedName, this.beanName);
			}
			// 如果 Bean 是 NullBean 实例
			if (bean instanceof NullBean) {
				// 将 bean 置 null
				bean = null;
			}
			// 返回解析出来的 bean
			return bean;
		}
		catch (BeansException ex) {
			throw new BeanCreationException(
					this.beanDefinition.getResourceDescription(), this.beanName,
					"Cannot resolve reference to bean '" + ref.getBeanName() + "' while setting " + argName, ex);
		}
	}

	/**
	 * Resolve an inner bean definition.
	 * @param argName the name of the argument that the inner bean is defined for
	 * @param innerBeanName the name of the inner bean
	 * @param innerBd the bean definition for the inner bean
	 * @return the resolved inner bean instance
	 */
	@Nullable
	private Object resolveInnerBean(Object argName, String innerBeanName, BeanDefinition innerBd) {
		RootBeanDefinition mbd = null;
		try {
			mbd = this.beanFactory.getMergedBeanDefinition(innerBeanName, innerBd, this.beanDefinition);
			// Check given bean name whether it is unique. If not already unique,
			// add counter - increasing the counter until the name is unique.
			String actualInnerBeanName = innerBeanName;
			if (mbd.isSingleton()) {
				actualInnerBeanName = adaptInnerBeanName(innerBeanName);
			}
			this.beanFactory.registerContainedBean(actualInnerBeanName, this.beanName);
			// Guarantee initialization of beans that the inner bean depends on.
			String[] dependsOn = mbd.getDependsOn();
			if (dependsOn != null) {
				for (String dependsOnBean : dependsOn) {
					this.beanFactory.registerDependentBean(dependsOnBean, actualInnerBeanName);
					this.beanFactory.getBean(dependsOnBean);
				}
			}
			// Actually create the inner bean instance now...
			Object innerBean = this.beanFactory.createBean(actualInnerBeanName, mbd, null);
			if (innerBean instanceof FactoryBean) {
				boolean synthetic = mbd.isSynthetic();
				innerBean = this.beanFactory.getObjectFromFactoryBean(
						(FactoryBean<?>) innerBean, actualInnerBeanName, !synthetic);
			}
			if (innerBean instanceof NullBean) {
				innerBean = null;
			}
			return innerBean;
		}
		catch (BeansException ex) {
			throw new BeanCreationException(
					this.beanDefinition.getResourceDescription(), this.beanName,
					"Cannot create inner bean '" + innerBeanName + "' " +
					(mbd != null && mbd.getBeanClassName() != null ? "of type [" + mbd.getBeanClassName() + "] " : "") +
					"while setting " + argName, ex);
		}
	}

	/**
	 * Checks the given bean name whether it is unique. If not already unique,
	 * a counter is added, increasing the counter until the name is unique.
	 * @param innerBeanName the original name for the inner bean
	 * @return the adapted name for the inner bean
	 */
	private String adaptInnerBeanName(String innerBeanName) {
		String actualInnerBeanName = innerBeanName;
		int counter = 0;
		String prefix = innerBeanName + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;
		while (this.beanFactory.isBeanNameInUse(actualInnerBeanName)) {
			counter++;
			actualInnerBeanName = prefix + counter;
		}
		return actualInnerBeanName;
	}

	/**
	 * For each element in the managed array, resolve reference if necessary.
	 */
	private Object resolveManagedArray(Object argName, List<?> ml, Class<?> elementType) {
		Object resolved = Array.newInstance(elementType, ml.size());
		for (int i = 0; i < ml.size(); i++) {
			Array.set(resolved, i, resolveValueIfNecessary(new KeyedArgName(argName, i), ml.get(i)));
		}
		return resolved;
	}

	/**
	 * For each element in the managed list, resolve reference if necessary.
	 */
	private List<?> resolveManagedList(Object argName, List<?> ml) {
		List<Object> resolved = new ArrayList<>(ml.size());
		for (int i = 0; i < ml.size(); i++) {
			resolved.add(resolveValueIfNecessary(new KeyedArgName(argName, i), ml.get(i)));
		}
		return resolved;
	}

	/**
	 * For each element in the managed set, resolve reference if necessary.
	 */
	private Set<?> resolveManagedSet(Object argName, Set<?> ms) {
		Set<Object> resolved = new LinkedHashSet<>(ms.size());
		int i = 0;
		for (Object m : ms) {
			resolved.add(resolveValueIfNecessary(new KeyedArgName(argName, i), m));
			i++;
		}
		return resolved;
	}

	/**
	 * For each element in the managed map, resolve reference if necessary.
	 */
	private Map<?, ?> resolveManagedMap(Object argName, Map<?, ?> mm) {
		Map<Object, Object> resolved = CollectionUtils.newLinkedHashMap(mm.size());
		mm.forEach((key, value) -> {
			Object resolvedKey = resolveValueIfNecessary(argName, key);
			Object resolvedValue = resolveValueIfNecessary(new KeyedArgName(argName, key), value);
			resolved.put(resolvedKey, resolvedValue);
		});
		return resolved;
	}


	/**
	 * Holder class used for delayed toString building.
	 */
	private static class KeyedArgName {

		private final Object argName;

		private final Object key;

		public KeyedArgName(Object argName, Object key) {
			this.argName = argName;
			this.key = key;
		}

		@Override
		public String toString() {
			return this.argName + " with key " + BeanWrapper.PROPERTY_KEY_PREFIX +
					this.key + BeanWrapper.PROPERTY_KEY_SUFFIX;
		}
	}

}
