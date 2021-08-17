package com.ztianzeng.apidoc.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.ztianzeng.apidoc.ModelResolver;
import com.ztianzeng.apidoc.SourceBuilder;

public abstract class TestBase {
	static ObjectMapper mapper;
	static SourceBuilder sourceBuilder = SourceBuilder.INSTANCE;
	static JavaProjectBuilder builder = sourceBuilder.getBuilder();

	public static ObjectMapper mapper() {
		if (mapper == null) {
			mapper = new ObjectMapper();
			mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
			mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		}
		return mapper;
	}

	protected ModelResolver modelResolver() {
		return new ModelResolver(SourceBuilder.INSTANCE);
	}

	protected void prettyPrint(Object o) {
		try {
			System.out.println(mapper().writer(new DefaultPrettyPrinter()).writeValueAsString(o));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
