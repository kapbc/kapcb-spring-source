package com.kapcb.ccc.components;

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
public class TestBean {

	private String username;

	public TestBean() {
		System.out.println("调用TestBean无参构造器");
	}

	public void say() {
		System.out.println("Hi, I'm " + this.username);
	}

	public void setUsername(String username) {
		System.out.println("调用TestBean中的setUsername方法注入属性");
		this.username = username;
	}
}
