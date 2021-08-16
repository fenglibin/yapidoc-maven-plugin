package com.ztianzeng.apidoc.test.swagger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

public class ResourceUtils {

	public static String loadClassResource(Class<?> cls, String name) throws IOException {
		InputStream in = null;
		try {
			in = cls.getClassLoader().getResourceAsStream(name);
			return IOUtils.toString(in, StandardCharsets.UTF_8);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
}
