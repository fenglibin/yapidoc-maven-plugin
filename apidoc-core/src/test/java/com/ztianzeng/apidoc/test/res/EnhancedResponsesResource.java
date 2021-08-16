package com.ztianzeng.apidoc.test.res;

import org.springframework.web.bind.annotation.GetMapping;

import lombok.Data;

public class EnhancedResponsesResource {

	/**
	 * Simple get operation
	 * <p>
	 * Defines a simple get operation with no inputs and a complex output object
	 * </p>
	 *
	 * @return voila!
	 */
	@GetMapping("/")
	@Deprecated
	public SampleResponseSchema getResponses() {
		return new SampleResponseSchema();
	}

	@Data
	static class SampleResponseSchema {
		/**
		 * the user id
		 */
		private String id;
	}

}
