package com.ztianzeng.apidoc.converter;

import java.util.Map;

import com.ztianzeng.apidoc.models.media.Schema;

public class ResolvedSchema {
	public Schema schema;
	public Map<String, Schema> referencedSchemas;
}
