package com.eeeffff.yapidoc.test.res;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 
 * @version V1.0
 * @date 2019-06-05 22:20
 */
@Data
public class Pet {
	/**
	 * The pet type
	 */
	@NotEmpty
	private String type;

	/**
	 * The name of the pet
	 */
	@NotEmpty
	private String name;

	@NotNull
	private Boolean isDomestic;
}