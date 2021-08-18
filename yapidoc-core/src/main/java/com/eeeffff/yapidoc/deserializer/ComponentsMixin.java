package com.eeeffff.yapidoc.deserializer;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.eeeffff.yapidoc.models.callbacks.Callback;

public abstract class ComponentsMixin {

	@JsonAnyGetter
	public abstract Map<String, Object> getExtensions();

	@JsonAnySetter
	public abstract void addExtension(String name, Object value);

	@JsonSerialize(contentUsing = CallbackSerializer.class)
	public abstract Map<String, Callback> getCallbacks();

}
