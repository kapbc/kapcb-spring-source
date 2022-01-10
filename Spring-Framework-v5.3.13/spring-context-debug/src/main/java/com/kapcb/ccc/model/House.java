package com.kapcb.ccc.model;

import java.math.BigDecimal;

/**
 * <a>Title: House </a>
 * <a>Author: Mike Chen <a>
 * <a>Description: House <a>
 *
 * @author Mike Chen
 * @date 2021/12/28 14:25
 */
public class House {

	private String location;

	private Integer size;

	private BigDecimal price;

	public House() {
	}

	public House(String location, Integer size, BigDecimal price) {
		this.location = location;
		this.size = size;
		this.price = price;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	@Override
	public String toString() {
		return "House{" +
				"location='" + location + '\'' +
				", size=" + size +
				", price=" + price +
				'}';
	}
}
