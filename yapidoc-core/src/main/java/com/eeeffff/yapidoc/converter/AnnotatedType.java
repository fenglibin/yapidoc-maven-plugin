package com.eeeffff.yapidoc.converter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.thoughtworks.qdox.model.JavaClass;
import com.eeeffff.yapidoc.models.media.Schema;
import com.eeeffff.yapidoc.utils.DocUtils;

public class AnnotatedType {
	private JavaClass javaClass;
	private String name;
	private Schema parent;
	private Function<AnnotatedType, Schema> jsonUnwrappedHandler;
	private boolean skipOverride;
	private boolean schemaProperty;
	private Annotation[] ctxAnnotations;
	private boolean resolveAsRef;
	private JsonView jsonViewAnnotation;
	private boolean skipSchemaName;
	private boolean skipJsonIdentity;
	private String propertyName;
	private JavaType javaType;

	public AnnotatedType() {
	}

	public AnnotatedType(JavaClass javaClass) {
		this.javaClass = javaClass;
	}

	public boolean isSkipOverride() {
		return skipOverride;
	}

	public void setSkipOverride(boolean skipOverride) {
		this.skipOverride = skipOverride;
	}

	public AnnotatedType skipOverride(boolean skipOverride) {
		this.skipOverride = skipOverride;
		return this;
	}

	public boolean isSkipJsonIdentity() {
		return skipJsonIdentity;
	}

	public void setSkipJsonIdentity(boolean skipJsonIdentity) {
		this.skipJsonIdentity = skipJsonIdentity;
	}

	public AnnotatedType skipJsonIdentity(boolean skipJsonIdentity) {
		this.skipJsonIdentity = skipJsonIdentity;
		return this;
	}

	public boolean isSkipSchemaName() {
		return skipSchemaName;
	}

	public void setSkipSchemaName(boolean skipSchemaName) {
		this.skipSchemaName = skipSchemaName;
	}

	public AnnotatedType skipSchemaName(boolean skipSchemaName) {
		this.skipSchemaName = skipSchemaName;
		return this;
	}

	public boolean isResolveAsRef() {
		return resolveAsRef;
	}

	public void setResolveAsRef(boolean resolveAsRef) {
		this.resolveAsRef = resolveAsRef;
	}

	public AnnotatedType resolveAsRef(boolean resolveAsRef) {
		this.resolveAsRef = resolveAsRef;
		return this;
	}

	public boolean isSchemaProperty() {
		return schemaProperty;
	}

	public void setSchemaProperty(boolean schemaProperty) {
		this.schemaProperty = schemaProperty;
	}

	public AnnotatedType schemaProperty(boolean schemaProperty) {
		this.schemaProperty = schemaProperty;
		return this;
	}

	public Function<AnnotatedType, Schema> getJsonUnwrappedHandler() {
		return jsonUnwrappedHandler;
	}

	public void setJsonUnwrappedHandler(Function<AnnotatedType, Schema> jsonUnwrappedHandler) {
		this.jsonUnwrappedHandler = jsonUnwrappedHandler;
	}

	public AnnotatedType jsonUnwrappedHandler(Function<AnnotatedType, Schema> jsonUnwrappedHandler) {
		this.jsonUnwrappedHandler = jsonUnwrappedHandler;
		return this;
	}

	public Schema getParent() {
		return parent;
	}

	public void setParent(Schema parent) {
		this.parent = parent;
	}

	public AnnotatedType parent(Schema parent) {
		this.parent = parent;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public AnnotatedType name(String name) {
		this.name = name;
		return this;
	}

	public Annotation[] getCtxAnnotations() {
		return ctxAnnotations;
	}

	public void setCtxAnnotations(Annotation[] ctxAnnotations) {
		this.ctxAnnotations = ctxAnnotations;
	}

	public AnnotatedType ctxAnnotations(Annotation[] ctxAnnotations) {
		setCtxAnnotations(ctxAnnotations);
		return this;
	}

	public JsonView getJsonViewAnnotation() {
		return jsonViewAnnotation;
	}

	public void setJsonViewAnnotation(JsonView jsonViewAnnotation) {
		this.jsonViewAnnotation = jsonViewAnnotation;
	}

	public AnnotatedType jsonViewAnnotation(JsonView jsonViewAnnotation) {
		this.jsonViewAnnotation = jsonViewAnnotation;
		return this;
	}

	public JavaClass getJavaClass() {
		return javaClass;
	}

	public AnnotatedType javaClass(JavaClass javaClass) {
		this.javaClass = javaClass;
		return this;
	}

	public JavaType getJavaType() {
		return javaType;
	}

	public AnnotatedType javaType(JavaType javaType) {
		this.javaType = javaType;
		return this;
	}

	/**
	 * @since 2.0.4
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * @since 2.0.4
	 */
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	/**
	 * @since 2.0.4
	 */
	public AnnotatedType propertyName(String propertyName) {
		this.propertyName = propertyName;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AnnotatedType)) {
			return false;
		}
		AnnotatedType that = (AnnotatedType) o;

		if (javaClass != null && that.javaClass != null && !javaClass.equals(that.javaClass)) {
			return false;
		}
		JavaClass aClass = DocUtils.genericityContentType(javaClass);
		if (aClass != null && that.javaClass != null && !aClass.equals(that.javaClass)) {
			return false;
		}
		return Arrays.equals(this.ctxAnnotations, that.ctxAnnotations);
	}

	@Override
	public int hashCode() {
		if (ctxAnnotations == null || ctxAnnotations.length == 0) {
			return Objects.hash(javaClass, "fixed");
		}
		List<Annotation> meaningfulAnnotations = new ArrayList<>();

		boolean hasDifference = false;
		for (Annotation a : ctxAnnotations) {
			if (!a.annotationType().getName().startsWith("sun") && !a.annotationType().getName().startsWith("jdk")) {
				meaningfulAnnotations.add(a);
			} else {
				hasDifference = true;
			}
		}
		int result = 1;
		result = 31 * result + (javaClass == null ? 0 : Objects.hash(javaClass, "fixed"));
		if (hasDifference) {
			result = 31 * result + (meaningfulAnnotations == null ? 0
					: Arrays.hashCode(meaningfulAnnotations.toArray(new Annotation[meaningfulAnnotations.size()])));
		} else {
			result = 31 * result + (ctxAnnotations == null ? 0 : Arrays.hashCode(ctxAnnotations));
		}
		return result;
	}

}
