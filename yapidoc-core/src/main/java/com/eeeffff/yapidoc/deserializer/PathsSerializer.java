package com.eeeffff.yapidoc.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.eeeffff.yapidoc.models.Paths;

public class PathsSerializer extends JsonSerializer<Paths> {

	private JsonSerializer<Object> defaultSerializer;

	public PathsSerializer() {
	}

	@Override
	public void serialize(Paths value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonProcessingException {

		if (value != null && value.getExtensions() != null && !value.getExtensions().isEmpty()) {
			jgen.writeStartObject();

			if (!value.isEmpty()) {
				for (String key : value.keySet()) {
					jgen.writeObjectField(key, value.get(key));
				}
			}
			for (String ext : value.getExtensions().keySet()) {
				jgen.writeObjectField(ext, value.getExtensions().get(ext));
			}
			jgen.writeEndObject();
		} else {
			provider.defaultSerializeValue(value, jgen);
		}
	}
}
