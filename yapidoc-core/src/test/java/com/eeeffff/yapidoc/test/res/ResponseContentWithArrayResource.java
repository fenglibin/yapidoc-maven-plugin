package com.eeeffff.yapidoc.test.res;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;

import lombok.Data;

public class ResponseContentWithArrayResource {

	@GetMapping("/user")
	public List<User> getUsers() {
		return null;
	}

	@Data
	public static class User {
		public String foo;
	}
}
