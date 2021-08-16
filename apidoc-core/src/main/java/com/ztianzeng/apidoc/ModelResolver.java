package com.ztianzeng.apidoc;

import static com.ztianzeng.apidoc.utils.DocUtils.genericityContentType;
import static com.ztianzeng.apidoc.utils.DocUtils.genericityCount;
import static com.ztianzeng.apidoc.utils.RefUtils.constructRef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaType;
import com.thoughtworks.qdox.model.impl.DefaultJavaParameterizedType;
import com.ztianzeng.apidoc.converter.AnnotatedType;
import com.ztianzeng.apidoc.converter.ModelConverter;
import com.ztianzeng.apidoc.converter.ModelConverterContext;
import com.ztianzeng.apidoc.models.media.ArraySchema;
import com.ztianzeng.apidoc.models.media.MapSchema;
import com.ztianzeng.apidoc.models.media.PrimitiveType;
import com.ztianzeng.apidoc.models.media.Schema;
import com.ztianzeng.apidoc.utils.DocUtils;
import com.ztianzeng.apidoc.utils.Json;

import lombok.extern.slf4j.Slf4j;

/**
 * 模型解析器
 *
 * @author zhaotianzeng
 * @version V1.0
 * @date 2019-06-06 13:00
 */
@Slf4j
public class ModelResolver implements ModelConverter {
	private JavaProjectBuilder builder;

	private final ObjectMapper mapper;

	public ModelResolver(SourceBuilder sourceBuilder) {
		builder = sourceBuilder.getBuilder();
		mapper = Json.mapper();
	}

	@Override
	public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
		if (annotatedType == null) {
			return null;
		}

		// 分析目标类信息
		JavaClass targetClass = annotatedType.getJavaClass();
		com.fasterxml.jackson.databind.JavaType targetType = mapper
				.constructType(DocUtils.getTypeForName(targetClass.getBinaryName()));

		if (annotatedType.getJavaType() != null) {
			targetType = annotatedType.getJavaType();
		}
		BeanDescription beanDesc = mapper.getSerializationConfig().introspect(targetType);

		Schema schema = new Schema();

		String parentName = findAnnotatedTypeName(annotatedType);
		// 看有没有被解析过，解析过直接返回
		Schema resolvedModel = context.resolve(annotatedType);
		if (resolvedModel != null) {
			if (parentName.equals(resolvedModel.getName())) {
				return resolvedModel;
			}
		}

		JavaClass genericityContentType = null;
		// 如果泛型大于0
		if (genericityCount(targetClass) > 0) {
			genericityContentType = genericityContentType(targetClass);
			if (genericityContentType != null) {
				AnnotatedType aType = new AnnotatedType().javaClass(genericityContentType).parent(schema)
						.resolveAsRef(annotatedType.isResolveAsRef())
						.jsonViewAnnotation(annotatedType.getJsonViewAnnotation()).skipSchemaName(true)
						.schemaProperty(true).propertyName(targetClass.getName());
				context.resolve(aType);
			}
		}

		if (targetClass.isEnum()) {
			for (JavaField enumConstant : targetClass.getEnumConstants()) {
				schema.addEnumItemObject(enumConstant.getComment() + " " + enumConstant.getName());
			}

			schema.setType(PrimitiveType.STRING.getCommonName());
		} else {
			// 转换成OpenApi定义的字段信息
			PrimitiveType parentType = PrimitiveType.fromType(targetClass.getBinaryName());
			schema.setType(Optional.ofNullable(parentType).orElse(PrimitiveType.OBJECT).getCommonName());
		}

		if (DocUtils.isPrimitive(targetClass.getName())) {
			if (targetClass.isArray()) {
				Schema array = new Schema();
				array.setType(schema.getType());
				schema = new ArraySchema().items(array);
			}
			return schema;
		}
		schema.name(parentName);

		// 分析类的字段
		List<JavaField> fields = new ArrayList<>();

		// 循环当前类信息，所有属性都加到fields
		JavaClass cls = targetClass;
		while (cls != null && !cls.isArray() && !"java.lang.Object".equals(cls.getFullyQualifiedName())) {
			fields.addAll(cls.getFields());
			cls = cls.getSuperJavaClass();
		}

		if (targetClass.isA(Map.class.getName())) {
			List<JavaType> tar = new LinkedList<>();
			if (targetClass instanceof DefaultJavaParameterizedType) {
				tar = ((DefaultJavaParameterizedType) targetClass).getActualTypeArguments();
			}
			// 泛型信息

			if (tar.isEmpty()) {
				return null;
			}
			JavaType javaType = tar.get(1);

			Schema addPropertiesSchema = context
					.resolve(new AnnotatedType().javaClass(builder.getClassByName(javaType.getFullyQualifiedName()))
							.schemaProperty(annotatedType.isSchemaProperty()).skipSchemaName(true)
							.resolveAsRef(annotatedType.isResolveAsRef())
							.jsonViewAnnotation(annotatedType.getJsonViewAnnotation())
							.propertyName(annotatedType.getPropertyName()).parent(annotatedType.getParent()));
			String pName = null;
			if (addPropertiesSchema != null) {
				if (StringUtils.isNotBlank(addPropertiesSchema.getName())) {
					pName = addPropertiesSchema.getName();
				}
				if ("object".equals(addPropertiesSchema.getType()) && pName != null) {
					// create a reference for the items
					if (context.getDefinedModels().containsKey(pName)) {
						addPropertiesSchema = new Schema().$ref(constructRef(pName));
					}
				} else if (addPropertiesSchema.get$ref() != null) {
					addPropertiesSchema = new Schema()
							.$ref(StringUtils.isNotEmpty(addPropertiesSchema.get$ref()) ? addPropertiesSchema.get$ref()
									: addPropertiesSchema.getName());
				}
			}
			schema = new MapSchema().additionalProperties(addPropertiesSchema);
		} else if (targetClass.isA(Collection.class.getName()) || targetClass.isA(List.class.getName())) {
			// 泛型信息
			List<JavaType> tar = ((DefaultJavaParameterizedType) targetClass).getActualTypeArguments();
			if (tar.isEmpty()) {
				return null;
			}
			JavaType javaType = tar.get(0);
			// 处理集合
			Schema items = context.resolve(new AnnotatedType()
					.javaClass(builder.getClassByName(javaType.getBinaryName()))
					.schemaProperty(annotatedType.isSchemaProperty()).skipSchemaName(true)
					.resolveAsRef(annotatedType.isResolveAsRef()).propertyName(annotatedType.getPropertyName())
					.jsonViewAnnotation(annotatedType.getJsonViewAnnotation()).parent(annotatedType.getParent()));

			if (items == null) {
				return null;
			}
			schema = new ArraySchema().items(items);
		}

		Map<String, JavaField> collect = fields.stream()
				.collect(Collectors.toMap(JavaField::getName, r -> r, (r1, r2) -> r1));

		for (BeanPropertyDefinition propertyDef : beanDesc.findProperties()) {
			JavaField field = collect.get(propertyDef.getName());
			if (field == null) {
				continue;
			}
			if (DocUtils.isPrimitive(field.getName())) {
				continue;
			}

			JavaClass type = field.getType();

			String typeName = findName(genericityContentType == null ? type : genericityContentType);
			AnnotatedType aType = new AnnotatedType().javaClass(type).javaType(propertyDef.getPrimaryType())
					.parent(schema).resolveAsRef(annotatedType.isResolveAsRef())
					.jsonViewAnnotation(annotatedType.getJsonViewAnnotation()).skipSchemaName(true).schemaProperty(true)
					.propertyName(targetClass.getName());
			// 属性是否为require
			boolean required = DocUtils.isRequired(field);
			// 属性名称
			String name = field.getName();
			Schema propSchema = new Schema();
			propSchema.setName(name);
			// 处理泛型
			if (genericityContentType != null && ("T".equals(field.getType().getGenericFullyQualifiedName())
					|| "java.lang.Object".equals(field.getType().getGenericFullyQualifiedName()))) {
				if (DocUtils.isList(genericityContentType.getBinaryName())) {
					aType = new AnnotatedType().javaClass(genericityContentType).parent(schema)
							.resolveAsRef(annotatedType.isResolveAsRef())
							.jsonViewAnnotation(annotatedType.getJsonViewAnnotation()).skipSchemaName(true)
							.schemaProperty(true).propertyName(targetClass.getName());
					propSchema = context.resolve(aType);
				} else {
					if (!DocUtils.isPrimitive(findName(genericityContentType))) {
						propSchema.set$ref(constructRef(findName(genericityContentType)));
					}

				}
			} else if (genericityContentType != null && DocUtils.isList(type.getBinaryName())) {
				aType = new AnnotatedType().javaClass(genericityContentType).parent(schema)
						.resolveAsRef(annotatedType.isResolveAsRef())
						.jsonViewAnnotation(annotatedType.getJsonViewAnnotation()).skipSchemaName(true)
						.schemaProperty(true).propertyName(targetClass.getName());
				propSchema = new ArraySchema().items(context.resolve(aType));

			} else {
				PrimitiveType primitiveType = PrimitiveType.fromType(field.getType().getFullyQualifiedName());
				if (primitiveType != null) {
					propSchema = primitiveType.createProperty();
				} else {
					propSchema = context.resolve(aType);
					if (propSchema != null) {
						if (propSchema.get$ref() == null) {
							if ("object".equals(propSchema.getType())) {
								// create a reference for the property
								if (!StringUtils.equals(propSchema.getName(), typeName)
										&& context.getDefinedModels().containsKey(typeName)) {
									propSchema.set$ref(constructRef(typeName));
								}
							}
						}
					}
				}
			}

			if (propSchema != null) {
				propSchema.setDescription(field.getComment());
			}

			if (required) {
				schema.addRequiredItem(name);
			}
			schema.addProperties(name, propSchema);
		}

		return schema;
	}

	public String findName(JavaClass type) {
		return findName(type, new StringBuilder());
	}

	/**
	 * 设置泛型
	 *
	 * @param type
	 */
	public String findName(JavaClass type, StringBuilder stringBuilder) {

		stringBuilder.append(type.getName());
		if (type instanceof DefaultJavaParameterizedType) {
			List<JavaType> ref = ((DefaultJavaParameterizedType) type).getActualTypeArguments();
			if (!ref.isEmpty()) {
				for (JavaType actualTypeArgument : ref) {
					if (actualTypeArgument instanceof DefaultJavaParameterizedType) {
						return findName((DefaultJavaParameterizedType) actualTypeArgument, stringBuilder);
					}
				}
			}
		}
		return stringBuilder.toString();
	}

	/**
	 * 获取类型上面的对应的名字
	 *
	 * @param annotatedType
	 * @return
	 */
	private String findAnnotatedTypeName(AnnotatedType annotatedType) {
		String parentName = annotatedType.getName();
		if (StringUtils.isBlank(parentName)) {
			parentName = findName(annotatedType.getJavaClass(), new StringBuilder());
		}
		return parentName;
	}

}