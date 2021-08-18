package com.eeeffff.yapidoc.test;

import javax.validation.constraints.NotEmpty;

import lombok.Data;

/**
 * 
 * @version V1.0
 * @date 2019-06-05 15:39
 */
@Data
public class CreateVO {
	/**
	 * 用户名
	 */
	@NotEmpty
	private String username;
	/**
	 * 手机
	 */
	private String mobile;
}