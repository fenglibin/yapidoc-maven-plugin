package com.eeeffff.yapidoc;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.eeeffff.yapidoc.models.headers.Header;

public class HeaderStyleEnumDeserializer extends JsonDeserializer<Header.StyleEnum> {
	@Override
	public Header.StyleEnum deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode node = jp.getCodec().readTree(jp);
		if (node != null) {
			String value = node.asText();
			return getStyleEnum(value);
		}
		return null;
	}

	private Header.StyleEnum getStyleEnum(String value) {
		return Arrays.stream(Header.StyleEnum.values()).filter(i -> i.toString().equals(value)).findFirst()
				.orElseThrow(() -> new RuntimeException(String.format(
						"Can not deserialize value of type Header.StyleEnum from String \"%s\": value not one of declared Enum instance names: %s",
						value, Arrays.stream(Header.StyleEnum.values()).map(v -> v.toString())
								.collect(Collectors.joining(", ", "[", "]")))));
	}
}
