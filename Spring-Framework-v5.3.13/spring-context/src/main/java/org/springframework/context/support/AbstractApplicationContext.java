/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.support.ResourceEditorRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationStartupAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.context.weaving.LoadTimeWeaverAwareProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.core.ResolvableType;
import org.springframework.core.SpringProperties;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract implementation of the {@link org.springframework.context.ApplicationContext}
 * interface. Doesn't mandate the type of storage used for configuration; simply
 * implements common context functionality. Uses the Template Method design pattern,
 * requiring concrete subclasses to implement abstract methods.
 *
 * <p>In contrast to a plain BeanFactory, an ApplicationContext is supposed
 * to detect special beans defined in its internal bean factory:
 * Therefore, this class automatically registers
 * {@link org.springframework.beans.factory.config.BeanFactoryPostProcessor BeanFactoryPostProcessors},
 * {@link org.springframework.beans.factory.config.BeanPostProcessor BeanPostProcessors},
 * and {@link org.springframework.context.ApplicationListener ApplicationListeners}
 * which are defined as beans in the context.
 *
 * <p>A {@link org.springframework.context.MessageSource} may also be supplied
 * as a bean in the context, with the name "messageSource"; otherwise, message
 * resolution is delegated to the parent context. Furthermore, a multicaster
 * for application events can be supplied as an "applicationEventMulticaster" bean
 * of type {@link org.springframework.context.event.ApplicationEventMulticaster}
 * in the context; otherwise, a default multicaster of type
 * {@link org.springframework.context.event.SimpleApplicationEventMulticaster} will be used.
 *
 * <p>Implements resource loading by extending
 * {@link org.springframework.core.io.DefaultResourceLoader}.
 * Consequently treats non-URL resource paths as class path resources
 * (supporting full class path resource names that include the package path,
 * e.g. "mypackage/myresource.dat"), unless the {@link #getResourceByPath}
 * method is overridden in a subclass.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see org.springframework.beans.factory.config.BeanFactoryPostProcessor
 * @see org.springframework.beans.factory.config.BeanPostProcessor
 * @see org.springframework.context.event.ApplicationEventMulticaster
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.MessageSource
 *
 * 1.实现 Closeble、AutoCloseble 接口
 * Closeble : JDK 提供的基于流的关闭
 * AutoCloseble : JDK7 提供, 支持 try() 自动关闭
 *
 * 2.实现 org.springframework.context.Lifecycle 接口
 * Lifecycle : 提供 start()、stop()、isRunning()方法
 *
 * 3.实现 org.springframework.context.ApplicationEventPublisher 接口
 * ApplicationEventPublisher : 事件发布体系
 * SimpleApplicationEventMulticaster : 负责注册、删除、与发布事件, 类似于观察者模式。注册事件支持 BeanName, 所以在创建 SimpleApplicationEventMulticaster 的时候需要一个 BeanFactory
 *
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext {

	// MessageSource 在此应用上下文中实例的名字, 如果没有对应实例, 消息解析功能会委托给其父应用上下文
	/**
	 * Name of the MessageSource bean in the factory.
	 * If none is supplied, message resolution is delegated to the parent.
	 * @see MessageSource
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	// LifecycleProcessor 类在此应用上下文中实例的名字。如果没有对应实例, 将会默认使用 DefaultLifecycleProcessor
	/**
	 * Name of the LifecycleProcessor bean in the factory.
	 * If none is supplied, a DefaultLifecycleProcessor is used.
	 * @see org.springframework.context.LifecycleProcessor
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 */
	public static final String LIFECYCLE_PROCESSOR_BEAN_NAME = "lifecycleProcessor";

	// ApplicationEventMulticaster 类在此应用中所对应的实例名字。如果没有对应实例, 则默认使用 SimpleApplicationEventMulticaster
	/**
	 * Name of the ApplicationEventMulticaster bean in the factory.
	 * If none is supplied, a default SimpleApplicationEventMulticaster is used.
	 * @see org.springframework.context.event.ApplicationEventMulticaster
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	/**
	 * Boolean flag controlled by a {@code spring.spel.ignore} system property that instructs Spring to
	 * ignore SpEL, i.e. to not initialize the SpEL infrastructure.
	 * <p>The default is "false".
	 */
	private static final boolean shouldIgnoreSpel = SpringProperties.getFlag("spring.spel.ignore");


	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		// 主动加载 ContextClosedEvent, 以避免在 WebLogic 8.1 中关闭应用时出现 ClassLoader 问题
		ContextClosedEvent.class.getName();
	}


	/** Logger used by this class. Available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	// 此实例对应的唯一 id, 名字 @hashcode 这种形式
	/** Unique id for this context, if any. */
	private String id = ObjectUtils.identityToString(this);

	// 用户好友的名字, 用以展示, 默认和 id 一样
	/** Display name. */
	private String displayName = ObjectUtils.identityToString(this);

	// 父应用上下文
	/** Parent context. */
	@Nullable
	private ApplicationContext parent;

	// 此应用上下文的环境变量实例
	/** Environment used by this context. */
	@Nullable
	private ConfigurableEnvironment environment;

	// 应用上下文在加载和刷新的时候需要使用到的 BeanFactoryPostProcessor
	/** BeanFactoryPostProcessors to apply on refresh. */
	private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();

	// 应用程序启动的时间, 时间戳
	/** System time in milliseconds when this context started. */
	private long startupDate;

	// 该应用是否处于激活状态的标识
	/** Flag that indicates whether this context is currently active. */
	private final AtomicBoolean active = new AtomicBoolean();

	// 该应用是否处于关闭的标识
	/** Flag that indicates whether this context has been closed already. */
	private final AtomicBoolean closed = new AtomicBoolean();

	// refresh 和 destroy 方法会 synchronize 到此对象上
	/** Synchronization monitor for the "refresh" and "destroy". */
	private final Object startupShutdownMonitor = new Object();

	// 提供给 JVM 的钩子方法, 用以 JVM 在关闭时同时关闭应用上下文, 清理资源
	/** Reference to the JVM shutdown hook, if registered. */
	@Nullable
	private Thread shutdownHook;

	// 此应用上下文使用的资源解析器
	/** ResourcePatternResolver used by this context. */
	private ResourcePatternResolver resourcePatternResolver;

	// 用以管理此应用上下文 Bean 生命周期的 lifecycle
	/** LifecycleProcessor for managing the lifecycle of beans within this context. */
	@Nullable
	private LifecycleProcessor lifecycleProcessor;

	// 用以处理此应用上下文的 MessageSource 实例
	/** MessageSource we delegate our implementation of this interface to. */
	@Nullable
	private MessageSource messageSource;

	// 用于事件发布的工具类
	/** Helper class used in event publishing. */
	@Nullable
	private ApplicationEventMulticaster applicationEventMulticaster;

	/** Application startup metrics. **/
	private ApplicationStartup applicationStartup = ApplicationStartup.DEFAULT;

	// 预留的 ApplicationListener
	/** Statically specified listeners. */
	private final Set<ApplicationListener<?>> applicationListeners = new LinkedHashSet<>();
	
	/** Local listeners registered before refresh. */
	@Nullable
	private Set<ApplicationListener<?>> earlyApplicationListeners;

	// 已经发布过的应用上下文事件
	/** ApplicationEvents published before the multicaster setup. */
	@Nullable
	private Set<ApplicationEvent> earlyApplicationEvents;


	/**
	 * Create a new AbstractApplicationContext with no parent.
	 * 
	 * 默认构造方法, 使用 PathMatchingResourcePatternResolver 作为资源解析器
	 */
	public AbstractApplicationContext() {
		this.resourcePatternResolver = getResourcePatternResolver();
	}

	/**
	 * Create a new AbstractApplicationContext with the given parent context.
	 * @param parent the parent context
	 * 
	 * 带有父上下文环境的构造方法                 
	 */
	public AbstractApplicationContext(@Nullable ApplicationContext parent) {
		this();
		setParent(parent);
	}

	//---------------------------------------------------------------------
	// Implementation of ApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * Set the unique id of this application context.
	 * <p>Default is the object id of the context instance, or the name
	 * of the context bean if the context is itself defined as a bean.
	 * @param id the unique id of the context
	 *
	 * 此对应上下文的唯一 id
	 * 对应的变量有默认值, 见声明
	 */
	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public String getApplicationName() {
		return "";
	}

	/**
	 * Set a friendly name for this context.
	 * Typically done during initialization of concrete context implementations.
	 * <p>Default is the object id of the context instance.
	 *
	 * 设置一个更友好的上下文名称, 通常在初始化应用上下文期间调用
	 */
	public void setDisplayName(String displayName) {
		Assert.hasLength(displayName, "Display name must not be empty");
		this.displayName = displayName;
	}

	/**
	 * Return a friendly name for this context.
	 * @return a display name for this context (never {@code null})
	 */
	@Override
	public String getDisplayName() {
		return this.displayName;
	}

	/**
	 * Return the parent context, or {@code null} if there is no parent
	 * (that is, this context is the root of the context hierarchy).
	 */
	@Override
	@Nullable
	public ApplicationContext getParent() {
		return this.parent;
	}

	/**
	 * Set the {@code Environment} for this application context.
	 * <p>Default value is determined by {@link #createEnvironment()}. Replacing the
	 * default with this method is one option but configuration through {@link
	 * #getEnvironment()} should also be considered. In either case, such modifications
	 * should be performed <em>before</em> {@link #refresh()}.
	 * @see org.springframework.context.support.AbstractApplicationContext#createEnvironment
	 */
	@Override
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	/**
	 * Return the {@code Environment} for this application context in configurable
	 * form, allowing for further customization.
	 * <p>If none specified, a default environment will be initialized via
	 * {@link #createEnvironment()}.
	 */
	@Override
	public ConfigurableEnvironment getEnvironment() {
		if (this.environment == null) {
			this.environment = createEnvironment();
		}
		return this.environment;
	}

	/**
	 * Create and return a new {@link StandardEnvironment}.
	 * <p>Subclasses may override this method in order to supply
	 * a custom {@link ConfigurableEnvironment} implementation.
	 */
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardEnvironment();
	}

	/**
	 * Return this context's internal bean factory as AutowireCapableBeanFactory,
	 * if already available.
	 * @see #getBeanFactory()
	 */
	@Override
	public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
		return getBeanFactory();
	}

	/**
	 * Return the timestamp (ms) when this context was first loaded.
	 */
	@Override
	public long getStartupDate() {
		return this.startupDate;
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be application-specific or a
	 * standard framework event)
	 */
	@Override
	public void publishEvent(ApplicationEvent event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementations cannot publish events.
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 */
	@Override
	public void publishEvent(Object event) {
		publishEvent(event, null);
	}

	/**
	 * Publish the given event to all listeners.
	 * @param event the event to publish (may be an {@link ApplicationEvent}
	 * or a payload object to be turned into a {@link PayloadApplicationEvent})
	 * @param eventType the resolved event type, if known
	 * @since 4.2
	 */
	protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
		Assert.notNull(event, "Event must not be null");

		// Decorate event as an ApplicationEvent if necessary
		// 将事件封装为 ApplicationEvent
		ApplicationEvent applicationEvent;
		if (event instanceof ApplicationEvent) {
			applicationEvent = (ApplicationEvent) event;
		}
		else {
			applicationEvent = new PayloadApplicationEvent<>(this, event);
			if (eventType == null) {
				eventType = ((PayloadApplicationEvent<?>) applicationEvent).getResolvableType();
			}
		}

		// Multicast right now if possible - or lazily once the multicaster is initialized
		if (this.earlyApplicationEvents != null) {
			this.earlyApplicationEvents.add(applicationEvent);
		}
		else {
			// 使用事件广播器, 广播事件
			getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
		}

		// Publish event via parent context as well...
		if (this.parent != null) {
			if (this.parent instanceof AbstractApplicationContext) {
				((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
			}
			else {
				this.parent.publishEvent(event);
			}
		}
	}

	/**
	 * Return the internal ApplicationEventMulticaster used by the context.
	 * @return the internal ApplicationEventMulticaster (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	ApplicationEventMulticaster getApplicationEventMulticaster() throws IllegalStateException {
		if (this.applicationEventMulticaster == null) {
			throw new IllegalStateException("ApplicationEventMulticaster not initialized - " +
					"call 'refresh' before multicasting events via the context: " + this);
		}
		return this.applicationEventMulticaster;
	}

	@Override
	public void setApplicationStartup(ApplicationStartup applicationStartup) {
		Assert.notNull(applicationStartup, "applicationStartup should not be null");
		this.applicationStartup = applicationStartup;
	}

	@Override
	public ApplicationStartup getApplicationStartup() {
		return this.applicationStartup;
	}

	/**
	 * Return the internal LifecycleProcessor used by the context.
	 * @return the internal LifecycleProcessor (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	LifecycleProcessor getLifecycleProcessor() throws IllegalStateException {
		if (this.lifecycleProcessor == null) {
			throw new IllegalStateException("LifecycleProcessor not initialized - " +
					"call 'refresh' before invoking lifecycle methods via the context: " + this);
		}
		return this.lifecycleProcessor;
	}

	/**
	 * Return the ResourcePatternResolver to use for resolving location patterns
	 * into Resource instances. Default is a
	 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver},
	 * supporting Ant-style location patterns.
	 * <p>Can be overridden in subclasses, for extended resolution strategies,
	 * for example in a web environment.
	 * <p><b>Do not call this when needing to resolve a location pattern.</b>
	 * Call the context's {@code getResources} method instead, which
	 * will delegate to the ResourcePatternResolver.
	 * @return the ResourcePatternResolver for this context
	 * @see #getResources
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext interface
	//---------------------------------------------------------------------

	/**
	 * Set the parent of this application context.
	 * <p>The parent {@linkplain ApplicationContext#getEnvironment() environment} is
	 * {@linkplain ConfigurableEnvironment#merge(ConfigurableEnvironment) merged} with
	 * this (child) application context environment if the parent is non-{@code null} and
	 * its environment is an instance of {@link ConfigurableEnvironment}.
	 * @see ConfigurableEnvironment#merge(ConfigurableEnvironment)
	 */
	@Override
	public void setParent(@Nullable ApplicationContext parent) {
		this.parent = parent;
		if (parent != null) {
			Environment parentEnvironment = parent.getEnvironment();
			if (parentEnvironment instanceof ConfigurableEnvironment) {
				getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
			}
		}
	}

	// 使用硬编码方式注入 BeanFactoryPostProcessor : 这种硬编码注入的 BeanFactoryPostProcessor 并不需要
	// 也不支持接口排序。 而使用配置注入的方式因为 Spring 无法保证加载的顺序, 所以支持通过 PriorityOrdered、
	// Ordered 排序接口的排序
	@Override
	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
		Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
		this.beanFactoryPostProcessors.add(postProcessor);
	}

	/**
	 * 返回通过 addBeanFactoryPostProcessor() 方法添加的用于内部 BeanFactory 的 BeanFactoryPostProcessor 集合
	 * @see AbstractApplicationContext#addBeanFactoryPostProcessor(BeanFactoryPostProcessor)
	 *
	 * Return the list of BeanFactoryPostProcessors that will get applied
	 * to the internal BeanFactory.
	 */
	public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
		// 将 beanFactoryPostProcessors 直接返回, 此 beanFactoryPostProcessors 集合中存放的是
		// 通过 add 方法添加的 BeanFactoryPostProcessor
		return this.beanFactoryPostProcessors;
	}

	@Override
	public void addApplicationListener(ApplicationListener<?> listener) {
		Assert.notNull(listener, "ApplicationListener must not be null");
		if (this.applicationEventMulticaster != null) {
			this.applicationEventMulticaster.addApplicationListener(listener);
		}
		this.applicationListeners.add(listener);
	}

	/**
	 * Return the list of statically specified ApplicationListeners.
	 */
	public Collection<ApplicationListener<?>> getApplicationListeners() {
		return this.applicationListeners;
	}

	/**
	 * 容器刷新, Spring中最为重要的方法
	 * @throws BeansException BeansException
	 * @throws IllegalStateException IllegalStateException
	 */
	@Override
	public void refresh() throws BeansException, IllegalStateException {
		// 进入同步代码块, 保证线程安全
		synchronized (this.startupShutdownMonitor) {
			StartupStep contextRefresh = this.applicationStartup.start("spring.context.refresh");

			// Prepare this context for refreshing.
			// 准备刷新上下文环境。作用就是初始化一些状态和属性, 为后续的工作做准备
			// 1.设置容器启动的时间
			// 2.设置容器活跃状态为 true
			// 3.设置容器关闭状态为 false
			// 4.获取 Environment 对象, 并加载当前系统的属性值到 Environment 对象中
			// 5.准备监听器和事件的集合对象, 默认为空集合
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			// 创建容器对象 : DefaultListableBeanFactory
			// 加载 xml 配置文件或者配置类的属性到当前 beanFactory 中。最重要的就是 BeanDefinition
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			// beanFactory的准备工作。对各种属性进行填充
			prepareBeanFactory(beanFactory);

			try {
				// Allows post-processing of the bean factory in context subclasses.
				// 子类覆盖方法做额外处理, 此处我们自己一般不做任何扩展工作。默认没有实现。但是可以查看 web 中的代码, 是有具体实现的。
				// 这个方法是模板方法设计模式的体现, 默认无任何实现, 预留给子类扩展使用
				postProcessBeanFactory(beanFactory);

				StartupStep beanPostProcess = this.applicationStartup.start("spring.context.beans.post-process");

				// Invoke factory processors registered as beans in the context.
				// 调用各种 BeanFactory 处理器, 其中最为关键的是 ConfigurationClassPostProcessor 在这里完成了配置类的解析
				// 生成的注入容器中的 Bean 的 BeanDefinition。
				invokeBeanFactoryPostProcessors(beanFactory);

				// Register bean processors that intercept bean creation.
				// 注册 BeanPostProcessor 处理器。这里只是单纯的注册功能, 真正调用的是 getBean 方法,
				// BeanPostProcessor 在这一步已经通过 beanFactory.getBean() 方法完成了创建。
				registerBeanPostProcessors(beanFactory);
				beanPostProcess.end();

				// Initialize message source for this context.
				// 为上下文初始化 Message 源, 即不同语言的消息体做国际化处理。
				initMessageSource();

				// Initialize event multicaster for this context.
				// 初始化事件监听多路广播器
				initApplicationEventMulticaster();

				// Initialize other special beans in specific context subclasses.
				// 预留给子类, 用于初始化其他的 Bean
				onRefresh();

				// Check for listener beans and register them.
				// 在所有注册的 Bean 中查找 listener beans 并注册到消息消息广播中
				registerListeners();

				// Instantiate all remaining (non-lazy-init) singletons.
				// 初始化剩下的单实例 Bean(非惰性)
				finishBeanFactoryInitialization(beanFactory);

				// Last step: publish corresponding event.
				// 完成容器刷新。通知生命周期处理器 lifecycle processor 刷新过程。同时发出 ContextRefreshEvent 告知别人容器已完成刷新。
				finishRefresh();
			}

			catch (BeansException ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Exception encountered during context initialization - " +
							"cancelling refresh attempt: " + ex);
				}

				// Destroy already created singletons to avoid dangling resources.
				// 防止 已经创建的 Bean 占用资源, 在异常处理中, 销毁在异常之前创建的单例 Bean
				destroyBeans();

				// Reset 'active' flag.
				// 重置容器 active 标志为 false 状态
				cancelRefresh(ex);

				// Propagate exception to caller.
				// 抛出异常
				throw ex;
			}

			finally {
				// Reset common introspection caches in Spring's core, since we
				// might not ever need metadata for singleton beans anymore...
				resetCommonCaches();
				contextRefresh.end();
			}
		}
	}

	/**
	 * Prepare this context for refreshing, setting its startup date and
	 * active flag as well as performing any initialization of property sources.
	 */
	protected void prepareRefresh() {
		// Switch to active.

		// 1.设置容器的启动时间
		this.startupDate = System.currentTimeMillis();

		// 2.设置容器关闭状态为 false
		this.closed.set(false);

		// 3.设置容器活跃状态为 true
		this.active.set(true);

		// 记录日志
		if (logger.isDebugEnabled()) {
			if (logger.isTraceEnabled()) {
				logger.trace("Refreshing " + this);
			}
			else {
				logger.debug("Refreshing " + getDisplayName());
			}
		}

		// Initialize any placeholder property sources in the context environment.
		// 留给子类覆盖, 初始化属性资源
		initPropertySources();

		// Validate that all properties marked as required are resolvable:
		// see ConfigurablePropertyResolver#setRequiredProperties
		// 创建并获取 Environment 对象, 验证系统需要的属性文件是否都已加载入 Environment 中
		getEnvironment().validateRequiredProperties();

		// Store pre-refresh ApplicationListeners...
		// 5.准备监听器和事件的集合对象, 默认为空集合
		// 判断刷新前的应用程序监听器集合是否为空, 如果为空则将监听器集合添加到此集合中
		if (this.earlyApplicationListeners == null) {
			this.earlyApplicationListeners = new LinkedHashSet<>(this.applicationListeners);
		}
		else {
			// Reset local application listeners to pre-refresh state.
			// 如果不为空, 则清空监听器集合元素
			this.applicationListeners.clear();
			this.applicationListeners.addAll(this.earlyApplicationListeners);
		}

		// Allow for the collection of early ApplicationEvents,
		// to be published once the multicaster is available...
		// 创建刷新前的事件监听集合
		this.earlyApplicationEvents = new LinkedHashSet<>();
	}

	/**
	 * <p>Replace any stub property sources with actual instances.
	 * @see org.springframework.core.env.PropertySource.StubPropertySource
	 * @see org.springframework.web.context.support.WebApplicationContextUtils#initServletPropertySources
	 */
	protected void initPropertySources() {
		// For subclasses: do nothing by default.
	}

	/**
	 * Tell the subclass to refresh the internal bean factory.
	 * @return the fresh BeanFactory instance
	 * @see #refreshBeanFactory()
	 * @see #getBeanFactory()
	 */
	protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
		// 初始化 beanFactory, 并进行配置读取(XML 或配置类), 并将获取的 beanFactory 记录在当前实体的属性中
		// 实际上是将 beanFactory 的创建委托给 refreshBeanFactory() 方法
		// refreshBeanFactory() 方法被两个类实现 AbstractRefreshableApplicationContext 和 GenericApplicationContext
		refreshBeanFactory();
		// 返回当前实体的 beanFactory 属性
		return getBeanFactory();
	}

	/**
	 * 配置工厂的标准上下文特征, 比如上下文类加载器和后置处理器
	 *
	 * Configure the factory's standard context characteristics,
	 * such as the context's ClassLoader and post-processors.
	 * @param beanFactory the BeanFactory to configure
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// Tell the internal bean factory to use the context's class loader etc.
		// 设置当前 beanFactory 的 classLoader 为当前 context 的 classLoader
		beanFactory.setBeanClassLoader(getClassLoader());
		if (!shouldIgnoreSpel) {
			// 设置 beanFactory 的表达式语言处理器(即 spel spring expression language 在 Spring3.x 中增加的表达式语言支持)。
			// 默认可以使用 #{bean.property} 的形式来调用获取bean的相关属性
			beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
		}
		// 为 beanFactory 增加一个默认的 propertyEditor, 这个主要是针对 bean 的属性等设置管理的一个工具
		beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

		// Configure the bean factory with context callbacks.
		// 添加 BeanPostProcessor。ApplicationContextAwareProcessor 类用来完成某些 Aware 对象的注入
		// 注意, 这里先为 ApplicationContext 容器中添加了 *Aware 接口相关的 BeanPostProcess, 在后面进行
		// 实例化时会激活这里添加的 BeanPostProcessor 并对下面设置忽略的接口进行所需依赖的注入操作
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));

		// 设置要忽略自动装配的接口
		// 为何要忽略自动装配, 因为这些接口的实现是由容器通过 set 方法进行注入的。
		// 所以在使用 @Autowired 注解进行注入的时候需要将这些接口进行忽略
		beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
		beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
		beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
		beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);
		beanFactory.ignoreDependencyInterface(ApplicationStartupAware.class);

		// BeanFactory interface not registered as resolvable type in a plain factory.
		// MessageSource registered (and found for autowiring) as a bean.
		// 设置几个自动装配的特殊规则, 当在进行 IOC 容器初始化过程中如果有多个实现, 那么就使用指定的对象进行依赖注入
		beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
		beanFactory.registerResolvableDependency(ResourceLoader.class, this);
		beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
		beanFactory.registerResolvableDependency(ApplicationContext.class, this);

		// Register early post-processor for detecting inner beans as ApplicationListeners.
		// 注册 BeanPostProcessor
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

		// Detect a LoadTimeWeaver and prepare for weaving, if found.
		// 增加对 Aspect J 的支持, 在 Java 中织入分为三种方式
		// 1.编译器织入
		// 编译器织入是指 : 在Java编译器采用特殊的编译器, 将切面织入到 Java 类中
		// 2.类加载器织入
		// 类加载器织入 : 通过特殊的类加载器, 在字节码加载到 JVM 中时, 织入切面
		// 3.运行期织入
		// 运行期织入 : 采用 CGLIB 和 JDK 进行切面的织入

		// Aspect J 提供了两种织入方式,
		// 第一种是通过编译器织入, 将使用 Aspect J 语言编写的切面类织入到 Java 类中
		// 第二种是通过类加载器织入, 就是下面的 loadTimeWeaver (LOAD_TIME_WEAVER_BEAN_NAME)
		if (!NativeDetector.inNativeImage() && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			// Set a temporary ClassLoader for type matching.
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}

		// Register default environment beans.
		// 注册默认的系统环境 Bean (单例)到一级缓存中
		if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
		}
		if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
			beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
		}
		if (!beanFactory.containsLocalBean(APPLICATION_STARTUP_BEAN_NAME)) {
			beanFactory.registerSingleton(APPLICATION_STARTUP_BEAN_NAME, getApplicationStartup());
		}
	}

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for registering special
	 * BeanPostProcessors etc in certain ApplicationContext implementations.
	 * @param beanFactory the bean factory used by the application context
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// 无实现, 预留给子类扩展使用
		// 模板方法设计模式体现
	}

	/**
	 * 根据顺序, 实例化并注册所有的 BeanFactoryPostProcessor Bean 实例
	 *
	 * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before singleton instantiation.
	 */
	protected void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// getBeanFactoryPostProcessors() 直接返回通过硬编码形式注册的 BeanFactoryPostProcessor 类型的处理器集合
		PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

		// Detect a LoadTimeWeaver and prepare for weaving, if found in the meantime
		// (e.g. through an @Bean method registered by ConfigurationClassPostProcessor)
		// Aspect J 提供了两种织入方式,
		// 第一种是通过编译器织入, 将使用 Aspect J 语言编写的切面类织入到 Java 类中
		// 第二种是通过类加载器织入, 就是下面的 loadTimeWeaver (LOAD_TIME_WEAVER_BEAN_NAME)
		if (!NativeDetector.inNativeImage() && beanFactory.getTempClassLoader() == null && beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
			beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
			beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
		}
	}

	/**
	 * Instantiate and register all BeanPostProcessor beans,
	 * respecting explicit order if given.
	 * <p>Must be called before any instantiation of application beans.
	 *
	 * 按照指定的顺序实例化并注册所有的 BeanPostProcessor Bean
	 */
	protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
		// 注册 BeanPostProcessor
		PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
	}

	/**
	 * 初始化 MessageSource, 如果当前容器中没有则使用父工厂的
	 * 提取配置中定义的 MessageSource, 并将其记录在 Spring 容器中, 也就是 AbstractApplicationContext 中
	 * 如果用户没有设置资源文件, Spring 提供了默认的配置 DelegatingMessageSource
	 * MessageSource 定义的 Bean 名字必须为 messageSource, 而如果找不到则
	 * 会默认注册 DelegatingMessageSource 作为 messageSource 的 Bean
	 *
	 * Initialize the MessageSource.
	 * Use parent's if none defined in this context.
	 */
	protected void initMessageSource() {
		// 获取 BeanFactory
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 如果容器中没有 BeanName 为 messageSource 的 Bean 实例,
		// 如果是自己注册的 MessageSource 组件, BeanName 一定要设置为 messageSource,
		// 否则就会使用 Spring 默认的 DelegatingMessageSource
		if (beanFactory.containsLocalBean(MESSAGE_SOURCE_BEAN_NAME)) {
			// 从容器中获取 BeanName 为 messageSource 类型为 MessageSource 的 Bean 实例, 并将其赋值给当前容器内部的 MessageSource
			this.messageSource = beanFactory.getBean(MESSAGE_SOURCE_BEAN_NAME, MessageSource.class);
			// Make MessageSource aware of parent MessageSource.
			// 判断父类是否不为空 && 当前对象的 messageSource 是 HierarchicalMessageSource 的实例
			if (this.parent != null && this.messageSource instanceof HierarchicalMessageSource) {
				// 将 messageSource 强制类型转换为 HierarchicalMessageSource 类型
				HierarchicalMessageSource hms = (HierarchicalMessageSource) this.messageSource;
				// 判断父 MessageSource 是否为空
				if (hms.getParentMessageSource() == null) {
					// Only set parent context as parent MessageSource if no parent MessageSource
					// registered already.
					// 将 HierarchicalMessageSource 的父 MessageSource 赋值为 getInternalParentMessageSource()
					hms.setParentMessageSource(getInternalParentMessageSource());
				}
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Using MessageSource [" + this.messageSource + "]");
			}
		}
		else {
			// Use empty MessageSource to be able to accept getMessage calls.
			// 容器中不存在 BeanName 为 messageSource 的 Bean 实例
			// 手动创建一个 DelegatingMessageSource 实例, 用于接受 getMessage 方法调用。
			DelegatingMessageSource dms = new DelegatingMessageSource();
			// 添加父类 MessageSource
			dms.setParentMessageSource(getInternalParentMessageSource());
			// 将 手动创建的 DelegatingMessageSource 赋值给当前容器中的 messageSource
			this.messageSource = dms;
			// 以 messageSource 为 BeanNme 将 DelegatingMessageSource 类的实例注册到一级缓存 singletonObjects 中
			beanFactory.registerSingleton(MESSAGE_SOURCE_BEAN_NAME, this.messageSource);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + MESSAGE_SOURCE_BEAN_NAME + "' bean, using [" + this.messageSource + "]");
			}
		}
	}

	/**
	 * 初始化事件监听多路广播器, 如果在容器中没有定义则使用 SimpleApplicationEventMulticaster
	 *
	 * ApplicationEventMulticaster 定义的 Bean 名字必须为 applicationEventMulticaster, 而如果找不到则
	 * Spring 会默认注册并使用 SimpleApplicationEventMulticaster 作为事件监听多路广播器
	 *
	 * Initialize the ApplicationEventMulticaster.
	 * Uses SimpleApplicationEventMulticaster if none defined in the context.
	 * @see org.springframework.context.event.SimpleApplicationEventMulticaster
	 */
	protected void initApplicationEventMulticaster() {
		// 获取 BeanFactory
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 从容器中获取 BeanName 为 applicationEventMulticaster 的 Bean 实例(如果用户自定义了事件广播器, 则使用用户自定义的事件广播器)
		if (beanFactory.containsLocalBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME)) {
			// 如果存在则从容器中获取 BeanName 为 applicationEventMulticaster 类型为 ApplicationEventMulticaster 的 Bean 实例, 并将其赋值为容器内部的事件广播器
			this.applicationEventMulticaster = beanFactory.getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, ApplicationEventMulticaster.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using ApplicationEventMulticaster [" + this.applicationEventMulticaster + "]");
			}
		}
		// 如果容器中不存在 BeanName 为 applicationEventMulticaster 的 Bean 实例, 则 Spring 会使用
		// SimpleApplicationEventMulticaster 作为当前容器内的默认事件广播器
		else {
			// 创建 SimpleApplicationEventMulticaster 事件广播器
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster(beanFactory);
			// 将其以 BeanName 为 applicationEventMulticaster 注册到 IOC 容器中
			beanFactory.registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, this.applicationEventMulticaster);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + APPLICATION_EVENT_MULTICASTER_BEAN_NAME + "' bean, using " +
						"[" + this.applicationEventMulticaster.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * Initialize the LifecycleProcessor.
	 * Uses DefaultLifecycleProcessor if none defined in the context.
	 * @see org.springframework.context.support.DefaultLifecycleProcessor
	 *
	 * 初始化 LifecycleProcessor
	 *
	 * 从容器中获取 BeanName 为 lifecycleProcessor 的 Bean 实例, 如果没有
	 * 则使用 Spring 默认的 DefaultLifecycleProcessor
	 */
	protected void initLifecycleProcessor() {
		// 获取 BeanFactory
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		// 如果工厂中包存在 BeanName 为 lifecycleProcessor 的 Bean
		if (beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
			// 获取该 Bean
			this.lifecycleProcessor =
					beanFactory.getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if (logger.isTraceEnabled()) {
				logger.trace("Using LifecycleProcessor [" + this.lifecycleProcessor + "]");
			}
		}
		else {
			// 工厂中不存在 BeanName 为 lifecycleProcessor 的 Bean
			// 使用 Spring 默认的 DefaultLifecycleProcessor
			DefaultLifecycleProcessor defaultProcessor = new DefaultLifecycleProcessor();
			// 设置 BeanFactory
			defaultProcessor.setBeanFactory(beanFactory);
			// 赋值给当前容器中的 lifecycleProcessor
			this.lifecycleProcessor = defaultProcessor;
			// 以 BeanNam 为 lifecycleProcessor 注册到容器中
			beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, this.lifecycleProcessor);
			if (logger.isTraceEnabled()) {
				logger.trace("No '" + LIFECYCLE_PROCESSOR_BEAN_NAME + "' bean, using " +
						"[" + this.lifecycleProcessor.getClass().getSimpleName() + "]");
			}
		}
	}

	/**
	 * Template method which can be overridden to add context-specific refresh work.
	 * Called on initialization of special beans, before instantiation of singletons.
	 * <p>This implementation is empty.
	 * @throws BeansException in case of errors
	 * @see #refresh()
	 */
	protected void onRefresh() throws BeansException {
		// For subclasses: do nothing by default.
	}

	/**
	 * Add beans that implement ApplicationListener as listeners.
	 * Doesn't affect other listeners, which can be added without being beans.
	 *
	 * 注册监听器
	 */
	protected void registerListeners() {
		// Register statically specified listeners first.
		// 使用硬编码方式注册的监听器
		for (ApplicationListener<?> listener : getApplicationListeners()) {
			getApplicationEventMulticaster().addApplicationListener(listener);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let post-processors apply to them!
		// 使用配置文件注册的监听器
		String[] listenerBeanNames = getBeanNamesForType(ApplicationListener.class, true, false);
		for (String listenerBeanName : listenerBeanNames) {
			getApplicationEventMulticaster().addApplicationListenerBean(listenerBeanName);
		}

		// Publish early application events now that we finally have a multicaster...
		// 发布之前保存的需要进行发布的事件
		Set<ApplicationEvent> earlyEventsToProcess = this.earlyApplicationEvents;
		this.earlyApplicationEvents = null;
		if (!CollectionUtils.isEmpty(earlyEventsToProcess)) {
			// 遍历事件依次发布
			for (ApplicationEvent earlyEvent : earlyEventsToProcess) {
				getApplicationEventMulticaster().multicastEvent(earlyEvent);
			}
		}
	}

	/**
	 * Finish the initialization of this context's bean factory,
	 * initializing all remaining singleton beans.
	 *
	 * 完成这个容器的 BeanFactory 初始化工作, 初始化剩下的单实例 Bean
	 */
	protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
		// Initialize conversion service for this context.
		// 为上下文初始化类型转换器
		if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) && beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
			beanFactory.setConversionService(beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
		}

		// Register a default embedded value resolver if no BeanFactoryPostProcessor
		// (such as a PropertySourcesPlaceholderConfigurer bean) registered any before:
		// at this point, primarily for resolution in annotation attribute values.
		// 如果 BeanFactory 之前没有注册嵌入值解析器, 则注册默认的嵌入值解析器, 主要用于注解值的解析
		if (!beanFactory.hasEmbeddedValueResolver()) {
			beanFactory.addEmbeddedValueResolver(strVal -> getEnvironment().resolvePlaceholders(strVal));
		}

		// Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
		// 尽早初始化 LoadTimeWeaverAware Bean, 以便尽早注册他们的转换器
		String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
		for (String weaverAwareName : weaverAwareNames) {
			getBean(weaverAwareName);
		}

		// Stop using the temporary ClassLoader for type matching.
		// 禁止使用临时类加载器进行类型匹配
		beanFactory.setTempClassLoader(null);

		// Allow for caching all bean definition metadata, not expecting further changes.
		// 冻结所有的 Bean 定义源数据信息。 说明注册的 BeanDefinition 将不被修改或任何进一步的处理
		beanFactory.freezeConfiguration();

		// Instantiate all remaining (non-lazy-init) singletons.
		// 实例化剩余的单实例对象
		beanFactory.preInstantiateSingletons();
	}

	/**
	 * Finish the refresh of this context, invoking the LifecycleProcessor's
	 * onRefresh() method and publishing the
	 * {@link org.springframework.context.event.ContextRefreshedEvent}.
	 */
	@SuppressWarnings("deprecation")
	protected void finishRefresh() {
		// Clear context-level resource caches (such as ASM metadata from scanning).
		// 清理容器级别的资源缓存
		clearResourceCaches();

		// Initialize lifecycle processor for this context.
		// 为上下文初始化生命周期处理器
		// ApplicationContext 在启动或停止时, 它会通过 LifecycleProcessor 来与
		// 所有声明的 Bean 的周期做状态更新, 这一步就是初始化 LifecycleProcessor
		initLifecycleProcessor();

		// Propagate refresh to lifecycle processor first.
		// 刷新所有实现了 Lifecycle 接口的 Bean
		getLifecycleProcessor().onRefresh();

		// Publish the final event.
		// 发布 ContextRefreshEvent 事件告知容器已完成刷新
		publishEvent(new ContextRefreshedEvent(this));

		// Participate in LiveBeansView MBean, if active.
		if (!NativeDetector.inNativeImage()) {
			LiveBeansView.registerApplicationContext(this);
		}
	}

	/**
	 * Cancel this context's refresh attempt, resetting the {@code active} flag
	 * after an exception got thrown.
	 * @param ex the exception that led to the cancellation
	 */
	protected void cancelRefresh(BeansException ex) {
		// 设置容器活跃状态为 false
		this.active.set(false);
	}

	/**
	 * Reset Spring's common reflection metadata caches, in particular the
	 * {@link ReflectionUtils}, {@link AnnotationUtils}, {@link ResolvableType}
	 * and {@link CachedIntrospectionResults} caches.
	 * @since 4.2
	 * @see ReflectionUtils#clearCache()
	 * @see AnnotationUtils#clearCache()
	 * @see ResolvableType#clearCache()
	 * @see CachedIntrospectionResults#clearClassLoader(ClassLoader)
	 */
	protected void resetCommonCaches() {



		ReflectionUtils.clearCache();
		AnnotationUtils.clearCache();
		ResolvableType.clearCache();
		CachedIntrospectionResults.clearClassLoader(getClassLoader());
	}


	/**
	 * Register a shutdown hook {@linkplain Thread#getName() named}
	 * {@code SpringContextShutdownHook} with the JVM runtime, closing this
	 * context on JVM shutdown unless it has already been closed at that time.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * @see Runtime#addShutdownHook
	 * @see ConfigurableApplicationContext#SHUTDOWN_HOOK_THREAD_NAME
	 * @see #close()
	 * @see #doClose()
	 */
	@Override
	public void registerShutdownHook() {
		if (this.shutdownHook == null) {
			// No shutdown hook registered yet.
			this.shutdownHook = new Thread(SHUTDOWN_HOOK_THREAD_NAME) {
				@Override
				public void run() {
					synchronized (startupShutdownMonitor) {
						doClose();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(this.shutdownHook);
		}
	}

	/**
	 * Callback for destruction of this instance, originally attached
	 * to a {@code DisposableBean} implementation (not anymore in 5.0).
	 * <p>The {@link #close()} method is the native way to shut down
	 * an ApplicationContext, which this method simply delegates to.
	 * @deprecated as of Spring Framework 5.0, in favor of {@link #close()}
	 */
	@Deprecated
	public void destroy() {
		close();
	}

	/**
	 * Close this application context, destroying all beans in its bean factory.
	 * <p>Delegates to {@code doClose()} for the actual closing procedure.
	 * Also removes a JVM shutdown hook, if registered, as it's not needed anymore.
	 * @see #doClose()
	 * @see #registerShutdownHook()
	 */
	@Override
	public void close() {
		synchronized (this.startupShutdownMonitor) {
			doClose();
			// If we registered a JVM shutdown hook, we don't need it anymore now:
			// We've already explicitly closed the context.
			if (this.shutdownHook != null) {
				try {
					Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
				}
				catch (IllegalStateException ex) {
					// ignore - VM is already shutting down
				}
			}
		}
	}

	/**
	 * Actually performs context closing: publishes a ContextClosedEvent and
	 * destroys the singletons in the bean factory of this application context.
	 * <p>Called by both {@code close()} and a JVM shutdown hook, if any.
	 * @see org.springframework.context.event.ContextClosedEvent
	 * @see #destroyBeans()
	 * @see #close()
	 * @see #registerShutdownHook()
	 */
	@SuppressWarnings("deprecation")
	protected void doClose() {
		// Check whether an actual close attempt is necessary...
		if (this.active.get() && this.closed.compareAndSet(false, true)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Closing " + this);
			}

			if (!NativeDetector.inNativeImage()) {
				LiveBeansView.unregisterApplicationContext(this);
			}

			try {
				// Publish shutdown event.
				publishEvent(new ContextClosedEvent(this));
			}
			catch (Throwable ex) {
				logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
			}

			// Stop all Lifecycle beans, to avoid delays during individual destruction.
			if (this.lifecycleProcessor != null) {
				try {
					this.lifecycleProcessor.onClose();
				}
				catch (Throwable ex) {
					logger.warn("Exception thrown from LifecycleProcessor on context close", ex);
				}
			}

			// Destroy all cached singletons in the context's BeanFactory.
			destroyBeans();

			// Close the state of this context itself.
			closeBeanFactory();

			// Let subclasses do some final clean-up if they wish...
			onClose();

			// Reset local application listeners to pre-refresh state.
			if (this.earlyApplicationListeners != null) {
				this.applicationListeners.clear();
				this.applicationListeners.addAll(this.earlyApplicationListeners);
			}

			// Switch to inactive.
			this.active.set(false);
		}
	}

	/**
	 * Template method for destroying all beans that this context manages.
	 * The default implementation destroy all cached singletons in this context,
	 * invoking {@code DisposableBean.destroy()} and/or the specified
	 * "destroy-method".
	 * <p>Can be overridden to add context-specific bean destruction steps
	 * right before or right after standard singleton destruction,
	 * while the context's BeanFactory is still active.
	 * @see #getBeanFactory()
	 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroySingletons()
	 */
	protected void destroyBeans() {
		getBeanFactory().destroySingletons();
	}

	/**
	 * Template method which can be overridden to add context-specific shutdown work.
	 * The default implementation is empty.
	 * <p>Called at the end of {@link #doClose}'s shutdown procedure, after
	 * this context's BeanFactory has been closed. If custom shutdown logic
	 * needs to execute while the BeanFactory is still active, override
	 * the {@link #destroyBeans()} method instead.
	 */
	protected void onClose() {
		// For subclasses: do nothing by default.
	}

	@Override
	public boolean isActive() {
		return this.active.get();
	}

	/**
	 * Assert that this context's BeanFactory is currently active,
	 * throwing an {@link IllegalStateException} if it isn't.
	 * <p>Invoked by all {@link BeanFactory} delegation methods that depend
	 * on an active context, i.e. in particular all bean accessor methods.
	 * <p>The default implementation checks the {@link #isActive() 'active'} status
	 * of this context overall. May be overridden for more specific checks, or for a
	 * no-op if {@link #getBeanFactory()} itself throws an exception in such a case.
	 */
	protected void assertBeanFactoryActive() {
		if (!this.active.get()) {
			if (this.closed.get()) {
				throw new IllegalStateException(getDisplayName() + " has been closed already");
			}
			else {
				throw new IllegalStateException(getDisplayName() + " has not been refreshed yet");
			}
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public Object getBean(String name) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name);
	}

	@Override
	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, requiredType);
	}

	@Override
	public Object getBean(String name, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(name, args);
	}

	@Override
	public <T> T getBean(Class<T> requiredType) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType);
	}

	@Override
	public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBean(requiredType, args);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType);
	}

	@Override
	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isSingleton(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isPrototype(name);
	}

	@Override
	public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().isTypeMatch(name, typeToMatch);
	}

	@Override
	@Nullable
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name);
	}

	@Override
	@Nullable
	public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
		assertBeanFactoryActive();
		return getBeanFactory().getType(name, allowFactoryBeanInit);
	}

	@Override
	public String[] getAliases(String name) {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	public boolean containsBeanDefinition(String beanName) {
		return getBeanFactory().containsBeanDefinition(beanName);
	}

	@Override
	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	@Override
	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanProvider(requiredType, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type);
	}

	@Override
	public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type);
	}

	@Override
	public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
	}

	@Override
	public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
		assertBeanFactoryActive();
		return getBeanFactory().getBeanNamesForAnnotation(annotationType);
	}

	@Override
	public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
			throws BeansException {

		assertBeanFactoryActive();
		return getBeanFactory().getBeansWithAnnotation(annotationType);
	}

	@Override
	@Nullable
	public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
			throws NoSuchBeanDefinitionException {

		assertBeanFactoryActive();
		return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory interface
	//---------------------------------------------------------------------

	@Override
	@Nullable
	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	@Override
	public boolean containsLocalBean(String name) {
		return getBeanFactory().containsLocalBean(name);
	}

	/**
	 * Return the internal bean factory of the parent context if it implements
	 * ConfigurableApplicationContext; else, return the parent context itself.
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	@Nullable
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource interface
	//---------------------------------------------------------------------

	@Override
	public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
		return getMessageSource().getMessage(code, args, defaultMessage, locale);
	}

	@Override
	public String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(code, args, locale);
	}

	@Override
	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return getMessageSource().getMessage(resolvable, locale);
	}

	/**
	 * Return the internal MessageSource used by the context.
	 * @return the internal MessageSource (never {@code null})
	 * @throws IllegalStateException if the context has not been initialized yet
	 */
	private MessageSource getMessageSource() throws IllegalStateException {
		if (this.messageSource == null) {
			throw new IllegalStateException("MessageSource not initialized - " +
					"call 'refresh' before accessing messages via the context: " + this);
		}
		return this.messageSource;
	}

	/**
	 * 判断父上下文是否为 AbstractApplicationContext 子类 :
	 * 如果是则返回父类上下文的内部 MessageSource
	 * 如果不是则返回父类上下文
	 *
	 * Return the internal message source of the parent context if it is an
	 * AbstractApplicationContext too; else, return the parent context itself.
	 */
	@Nullable
	protected MessageSource getInternalParentMessageSource() {
		// 判断父上下文是否为 AbstractApplicationContext 子类,
		// 如果是则返回父类上下文的内部 MessageSource, 如果不是则返回父类上下文
		return (getParent() instanceof AbstractApplicationContext ?
				((AbstractApplicationContext) getParent()).messageSource : getParent());
	}


	//---------------------------------------------------------------------
	// Implementation of ResourcePatternResolver interface
	//---------------------------------------------------------------------

	@Override
	public Resource[] getResources(String locationPattern) throws IOException {
		return this.resourcePatternResolver.getResources(locationPattern);
	}


	//---------------------------------------------------------------------
	// Implementation of Lifecycle interface
	//---------------------------------------------------------------------

	@Override
	public void start() {
		getLifecycleProcessor().start();
		publishEvent(new ContextStartedEvent(this));
	}

	@Override
	public void stop() {
		getLifecycleProcessor().stop();
		publishEvent(new ContextStoppedEvent(this));
	}

	@Override
	public boolean isRunning() {
		return (this.lifecycleProcessor != null && this.lifecycleProcessor.isRunning());
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * 将 BeanFactory 的创建委派给 refreshBeanFactory() 由子类继承并重写该方法去实现 BeanFactory
	 * 的创建, 自己并不关心创建过程
	 *
	 * Subclasses must implement this method to perform the actual configuration load.
	 * The method is invoked by {@link #refresh()} before any other initialization work.
	 * <p>A subclass will either create a new bean factory and hold a reference to it,
	 * or return a single BeanFactory instance that it holds. In the latter case, it will
	 * usually throw an IllegalStateException if refreshing the context more than once.
	 * @throws BeansException if initialization of the bean factory failed
	 * @throws IllegalStateException if already initialized and multiple refresh
	 * attempts are not supported
	 */
	protected abstract void refreshBeanFactory() throws BeansException, IllegalStateException;

	/**
	 * Subclasses must implement this method to release their internal bean factory.
	 * This method gets invoked by {@link #close()} after all other shutdown work.
	 * <p>Should never throw an exception but rather log shutdown failures.
	 */
	protected abstract void closeBeanFactory();

	/**
	 * Subclasses must return their internal bean factory here. They should implement the
	 * lookup efficiently, so that it can be called repeatedly without a performance penalty.
	 * <p>Note: Subclasses should check whether the context is still active before
	 * returning the internal bean factory. The internal factory should generally be
	 * considered unavailable once the context has been closed.
	 * @return this application context's internal bean factory (never {@code null})
	 * @throws IllegalStateException if the context does not hold an internal bean factory yet
	 * (usually if {@link #refresh()} has never been called) or if the context has been
	 * closed already
	 * @see #refreshBeanFactory()
	 * @see #closeBeanFactory()
	 *
	 * 获取当前上下文的 BeanFactory 对象
	 * 子类必须在此返回其内部 Bean 工厂, 他们应该有效地实现查找, 以便可以重复调用它而不影响性能
	 * 子类应在返回内部 Bean 工厂之前校验上下文是否仍处于活跃状态。一旦关闭上下文, 通常应将内部工厂视为不可用
	 * 此引用程序上下文的内部 Bean 工厂(永远不为 null)
	 * 如果上下文尚未拥有内部 Bean 工厂(通常是从未调用过 refresh())或者是上下文已经关闭
	 */
	@Override
	public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;


	/**
	 * Return information about this context.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getDisplayName());
		sb.append(", started on ").append(new Date(getStartupDate()));
		ApplicationContext parent = getParent();
		if (parent != null) {
			sb.append(", parent: ").append(parent.getDisplayName());
		}
		return sb.toString();
	}

}
