package com.kapcb.ccc.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * <a>Title: PersonFactoryBean </a>
 * <a>Author: Kapcb <a>
 * <a>Description: PersonFactoryBean <a>
 *
 * @author Kapcb
 * @version 1.0
 * @date 2022/1/10 22:12
 * @since 1.0
 */
public class PersonFactoryBean implements FactoryBean<Person> {

	private final Log log = LogFactory.getLog(getClass());

	@Override
	public Person getObject() throws Exception {
		log.info("Spring 调用 getBean 方法初始化 FactoryBean 实例");
		Person person = new Person();

		House shenZhen = new House();
		shenZhen.setLocation("南山");
		shenZhen.setPrice(BigDecimal.valueOf(100000000));
		shenZhen.setSize(500);

		House shanghai = new House();
		shanghai.setLocation("浦东");
		shanghai.setPrice(BigDecimal.valueOf(80000000));
		shanghai.setSize(300);

		person.setHouses(new HashMap<String, House>(2) {{
			put("深圳", shenZhen);
			put("上海", shanghai);
		}});

		person.setGirlFriends(new String[]{"aaa", "bbb", "ccc"});

		person.setBoyFriends(Arrays.asList("aaa1", "bbb1", "ccc1"));

		person.setPhoneNumbers(new HashSet<>(Arrays.asList("666666", "88888888")));

		School school = new School();
		school.setLocation("武汉");
		school.setSchoolName("武汉大学");
		person.setSchool(school);

		person.setAge(17);
		log.info("初始化成功");
		return person;
	}

	@Override
	public Class<?> getObjectType() {
		log.info("Spring 调用 PersonFactoryBean#getObjectType() 方法");
		return Person.class;
	}

	@Override
	public boolean isSingleton() {
		log.info("Spring 调用 PersonFactoryBean#isSingleton() 方法");
		return true;
	}

}
