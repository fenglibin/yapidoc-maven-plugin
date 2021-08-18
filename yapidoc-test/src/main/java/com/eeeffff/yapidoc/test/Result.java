package com.eeeffff.yapidoc.test;

import lombok.Getter;

/**
 * 
 * @version V1.0
 * @date 2019-06-11 13:13
 */
@Getter
public class Result<T> {
	private String msg;
	private T data;

	protected Result() {
	}

	private Result(String msg, T t) {
		this.msg = msg;
		this.data = t;
	}

}