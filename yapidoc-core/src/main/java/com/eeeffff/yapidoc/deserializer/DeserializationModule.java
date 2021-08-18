package com.eeeffff.yapidoc.deserializer;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.eeeffff.yapidoc.EncodingPropertyStyleEnumDeserializer;
import com.eeeffff.yapidoc.EncodingStyleEnumDeserializer;
import com.eeeffff.yapidoc.HeaderStyleEnumDeserializer;
import com.eeeffff.yapidoc.models.Paths;
import com.eeeffff.yapidoc.models.callbacks.Callback;
import com.eeeffff.yapidoc.models.headers.Header;
import com.eeeffff.yapidoc.models.media.Encoding;
import com.eeeffff.yapidoc.models.media.EncodingProperty;
import com.eeeffff.yapidoc.models.media.Schema;
import com.eeeffff.yapidoc.models.parameters.Parameter;
import com.eeeffff.yapidoc.models.responses.ApiResponses;
import com.eeeffff.yapidoc.models.security.SecurityScheme;

public class DeserializationModule extends SimpleModule {

	public DeserializationModule() {

		this.addDeserializer(Schema.class, new ModelDeserializer());
		this.addDeserializer(Parameter.class, new ParameterDeserializer());
		this.addDeserializer(Header.StyleEnum.class, new HeaderStyleEnumDeserializer());
		this.addDeserializer(Encoding.StyleEnum.class, new EncodingStyleEnumDeserializer());
		this.addDeserializer(EncodingProperty.StyleEnum.class, new EncodingPropertyStyleEnumDeserializer());

		this.addDeserializer(SecurityScheme.class, new SecuritySchemeDeserializer());

		this.addDeserializer(ApiResponses.class, new ApiResponsesDeserializer());
		this.addDeserializer(Paths.class, new PathsDeserializer());
		this.addDeserializer(Callback.class, new CallbackDeserializer());
	}
}
