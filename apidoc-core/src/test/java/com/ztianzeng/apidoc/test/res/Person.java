package com.ztianzeng.apidoc.test.res;

import java.util.Date;
import java.util.Map;

import com.ztianzeng.apidoc.models.Address;

import lombok.Data;

@Data
public class Person {
	private Long id;
	private String firstName;
	private Address address;
	private Map<String, String> properties;
	private Date birthDate;
	private Float floatValue;
	private Double doubleValue;

}
