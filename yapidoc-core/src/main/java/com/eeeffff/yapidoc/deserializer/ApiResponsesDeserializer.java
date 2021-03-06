package com.eeeffff.yapidoc.deserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.eeeffff.yapidoc.models.responses.ApiResponse;
import com.eeeffff.yapidoc.models.responses.ApiResponses;
import com.eeeffff.yapidoc.utils.Json;

public class ApiResponsesDeserializer extends JsonDeserializer<ApiResponses> {
	@Override
	public ApiResponses deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		ApiResponses result = new ApiResponses();
		JsonNode node = jp.getCodec().readTree(jp);
		ObjectNode objectNode = (ObjectNode) node;
		Map<String, Object> extensions = new LinkedHashMap<>();
		for (Iterator<String> it = objectNode.fieldNames(); it.hasNext();) {
			String childName = it.next();
			JsonNode child = objectNode.get(childName);
			// if name start with `x-` consider it an extesion
			if (childName.startsWith("x-")) {
				extensions.put(childName, Json.mapper().convertValue(child, Object.class));
			} else {
				result.put(childName, Json.mapper().convertValue(child, ApiResponse.class));
			}
		}
		if (!extensions.isEmpty()) {
			result.setExtensions(extensions);
		}
		return result;
	}
}
