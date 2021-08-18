package com.eeeffff.yapidoc.converter;

import java.util.Map;

import com.eeeffff.yapidoc.models.media.Schema;

public class ResolvedSchema {
	public Schema schema;
	public Map<String, Schema> referencedSchemas;
}
