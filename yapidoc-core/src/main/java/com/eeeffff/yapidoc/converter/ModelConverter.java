package com.eeeffff.yapidoc.converter;

import java.util.Iterator;

import com.eeeffff.yapidoc.models.media.Schema;

/**
 * 模型解析
 */
public interface ModelConverter {

	/**
	 * 解析
	 *
	 * @param type
	 * @param context
	 * @param chain   the chain of model converters to try if this implementation
	 *                cannot process
	 * @return null if this ModelConverter cannot convert the given Type
	 */
	Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain);
}
