package com.kapcb.ccc.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <a>Title: TestBean </a>
 * <a>Author: Kapcb <a>
 * <a>Description: TestBean <a>
 *
 * @author Kapcb
 * @version 1.0
 * @date 2021/12/11 23:21
 * @since 1.0
 */
public class TestBean implements DebugBean {

	protected final Log log = LogFactory.getLog(getClass());

	private String username;

	public TestBean() {
		log.info("调用TestBean无参构造器");
	}

	@Override
	public void say() {
		log.info("Hi, I'm " + this.username);
	}

	public void setUsername(String username) {
		log.info("调用TestBean中的setUsername方法注入属性");
		this.username = username;
	}

	@Override
	public String toString() {
		return "TestBean{" +
				"username='" + username + '\'' +
				'}';
	}

}
