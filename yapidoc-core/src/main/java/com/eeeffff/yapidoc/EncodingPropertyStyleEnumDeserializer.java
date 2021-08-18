package com.eeeffff.yapidoc;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.eeeffff.yapidoc.models.media.EncodingProperty;

public class EncodingPropertyStyleEnumDeserializer extends JsonDeserializer<EncodingProperty.StyleEnum> {
	@Override
	public EncodingProperty.StyleEnum deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode node = jp.getCodec().readTree(jp);
		if (node != null) {
			String value = node.asText();
			return getStyleEnum(value);
		}
		return null;
	}

	private EncodingProperty.StyleEnum getStyleEnum(String value) {
		return Arrays.stream(EncodingProperty.StyleEnum.values()).filter(i -> i.toString().equals(value)).findFirst()
				.orElseThrow(() -> new RuntimeException(String.format(
						"Can not deserialize value of type EncodingProperty.StyleEnum from String \"%s\": value not one of declared Enum instance names: %s",
						value, Arrays.stream(EncodingProperty.StyleEnum.values()).map(v -> v.toString())
								.collect(Collectors.joining(", ", "[", "]")))));
	}
}
