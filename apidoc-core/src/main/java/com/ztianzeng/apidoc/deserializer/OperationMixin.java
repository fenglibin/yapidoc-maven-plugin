package com.ztianzeng.apidoc.deserializer;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ztianzeng.apidoc.models.callbacks.Callback;
import com.ztianzeng.apidoc.models.responses.ApiResponses;

public abstract class OperationMixin {

	@JsonAnyGetter
	public abstract Map<String, Object> getExtensions();

	@JsonAnySetter
	public abstract void addExtension(String name, Object value);

	@JsonSerialize(contentUsing = CallbackSerializer.class)
	public abstract Map<String, Callback> getCallbacks();

	@JsonSerialize(using = ApiResponsesSerializer.class)
	public abstract ApiResponses getResponses();

}
