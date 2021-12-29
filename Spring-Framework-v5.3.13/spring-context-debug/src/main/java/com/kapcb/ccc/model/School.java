package com.kapcb.ccc.model;

/**
 * <a>Title: School </a>
 * <a>Author: Mike Chen <a>
 * <a>Description: School <a>
 *
 * @author Mike Chen
 * @date 2021/12/28 10:36
 */
public class School {

	private String schoolName;

	private String location;

	public School() {
	}

	public School(String schoolName, String location) {
		this.schoolName = schoolName;
		this.location = location;
	}

	public String getSchoolName() {
		return schoolName;
	}

	public void setSchoolName(String schoolName) {
		this.schoolName = schoolName;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

}
