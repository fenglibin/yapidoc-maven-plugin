package com.ztianzeng.apidoc;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public abstract class ExtensionsMixin {

	@JsonAnyGetter
	public abstract Map<String, Object> getExtensions();

	@JsonAnySetter
	public abstract void addExtension(String name, Object value);
}
