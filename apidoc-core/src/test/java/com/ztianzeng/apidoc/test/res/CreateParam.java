package com.ztianzeng.apidoc.test.res;

import javax.validation.constraints.NotEmpty;

import lombok.Data;

/**
 * @author zhaotianzeng
 * @version V1.0
 * @date 2019-05-27 13:37
 */
@Data
public class CreateParam {
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