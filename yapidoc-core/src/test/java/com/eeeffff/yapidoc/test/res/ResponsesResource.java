package com.eeeffff.yapidoc.test.res;

import org.springframework.web.bind.annotation.GetMapping;

import lombok.Data;

public class ResponsesResource {

	/**
	 * @return voila!
	 */
	@GetMapping("/")
	public SampleResponseSchema getResponses() {
		return new SampleResponseSchema();
	}

	@Data
	public static class SampleResponseSchema {
		private String id;
	}

}
