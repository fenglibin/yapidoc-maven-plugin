package com.eeeffff.yapidoc.test.res;

import org.springframework.web.bind.annotation.GetMapping;

public class DeprecatedFieldsResource {
	@GetMapping("/")
	@Deprecated
	public void deprecatedMethod() {

	}
}