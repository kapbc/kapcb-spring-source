package com.kapcb.ccc.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <a>Title: Person </a>
 * <a>Author: Mike Chen <a>
 * <a>Description: Person <a>
 *
 * @author Mike Chen
 * @date 2021/12/28 10:35
 */
public class Person {

	private final Log log = LogFactory.getLog(getClass());

	private String name;

	private int age;

	private School school;

	private String[] girlFriends;

	private List<String> boyFriends;

	private Set<String> phoneNumbers;

	private Map<String, House> houses;

	private DebugBean debugBean;

	public Person() {
	}

	public Person(String name, int age, School school, String[] girlFriends, List<String> boyFriends, Set<String> phoneNumbers, Map<String, House> houses, DebugBean debugBean) {
		this.name = name;
		this.age = age;
		this.school = school;
		this.girlFriends = girlFriends;
		this.boyFriends = boyFriends;
		this.phoneNumbers = phoneNumbers;
		this.houses = houses;
		this.debugBean = debugBean;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public School getSchool() {
		return school;
	}

	public void setSchool(School school) {
		this.school = school;
	}

	public String[] getGirlFriends() {
		return girlFriends;
	}

	public void setGirlFriends(String[] girlFriends) {
		this.girlFriends = girlFriends;
	}

	public List<String> getBoyFriends() {
		return boyFriends;
	}

	public void setBoyFriends(List<String> boyFriends) {
		this.boyFriends = boyFriends;
	}

	public Set<String> getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(Set<String> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	public Map<String, House> getHouses() {
		return houses;
	}

	public void setHouses(Map<String, House> houses) {
		this.houses = houses;
	}

	public DebugBean getDebugBean() {
		return debugBean;
	}

	public void setDebugBean(DebugBean debugBean) {
		log.info("Spring 调用 setDebugBean 方法为 Person 注入属性");
		this.debugBean = debugBean;
	}

	@Override
	public String toString() {
		return "Person{" +
				"name='" + name + '\'' +
				", age=" + age +
				", school=" + school +
				", girlFriends=" + Arrays.toString(girlFriends) +
				", boyFriends=" + boyFriends +
				", phoneNumbers=" + phoneNumbers +
				", houses=" + houses +
				", debugBean=" + debugBean +
				'}';
	}
}
