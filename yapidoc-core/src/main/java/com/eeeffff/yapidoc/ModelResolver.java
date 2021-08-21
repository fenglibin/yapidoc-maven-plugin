package com.eeeffff.yapidoc;

import static com.eeeffff.yapidoc.utils.DocUtils.genericityContentType;
import static com.eeeffff.yapidoc.utils.DocUtils.genericityCount;
import static com.eeeffff.yapidoc.utils.RefUtils.constructRef;

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
import com.thoughtworks.qdox.model.impl.DefaultJavaClass;
import com.thoughtworks.qdox.model.impl.DefaultJavaParameterizedType;
import com.eeeffff.yapidoc.converter.AnnotatedType;
import com.eeeffff.yapidoc.converter.ModelConverter;
import com.eeeffff.yapidoc.converter.ModelConverterContext;
import com.eeeffff.yapidoc.models.media.ArraySchema;
import com.eeeffff.yapidoc.models.media.MapSchema;
import com.eeeffff.yapidoc.models.media.PrimitiveType;
import com.eeeffff.yapidoc.models.media.Schema;
import com.eeeffff.yapidoc.utils.DocUtils;
import com.eeeffff.yapidoc.utils.Json;

import lombok.extern.slf4j.Slf4j;

/**
 * 模型解析器
 *
 * 
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
		if (targetClass == null) {
			log.warn("annotatedType:" + annotatedType.getClass() + "获取到的Java class为空！");
		}
		try {
			targetClass.isEnum();
			targetClass.getName();
		} catch (Exception e) {
			log.debug("获取目标类:" + targetClass.getClass() + " 是否枚举类型或名称发生异常，将该类替换为java.lang.Object类");
			targetClass = SourceBuilder.INSTANCE.getBuilder().getClassByName("java.lang.Object");
		}

		com.fasterxml.jackson.databind.JavaType targetType = null;
		try {
			targetType = mapper.constructType(DocUtils.getTypeForName(targetClass.getBinaryName()));
		} catch (Exception e) {
			log.debug("TargetClass:" + targetClass.getClass() + " binary name is:" + targetClass.getBinaryName()
					+ " 找不到对应的类型，使用Object代替");
			targetType = mapper.constructType(DocUtils.getTypeForName("java.lang.Object"));
		}

		if (annotatedType.getJavaType() != null) {
			targetType = annotatedType.getJavaType();
		}

		// 读取指定Bean的属性，并构造成一个用于后续处理的BeanDescription
		// 问题１：如果参数为List和Map时，这两个对象本身都没有属性值
		BeanDescription beanDesc = mapper.getSerializationConfig().introspect(targetType);

		// 定义输入和输出数据类型的模式对象，这些类型可以是对象，也可以是基本数据类型、数组等。
		Schema schema = new Schema();

		String parentName = findAnnotatedTypeName(annotatedType);
		// 解析该类型，如果已经解析过则直接返回，如果没有解析过则执行解析
		Schema resolvedModel = context.resolve(annotatedType);
		if (resolvedModel != null) {
			if (parentName.equals(resolvedModel.getName())) {
				return resolvedModel;
			}
		}

		schema.name(parentName);
		//schema.setType(targetClass.getBinaryName());
		// 拼装泛型类型字符串
		String fileTypeInfo = DocUtils.getParameterFiledInfo(targetClass);
		schema.setDescription("参数类型：" + fileTypeInfo);

		JavaClass genericityContentType = null;
		// 如果泛型大于0
		if (genericityCount(targetClass) > 0) {
			genericityContentType = genericityContentType(targetClass);
			if (genericityContentType != null) {
				AnnotatedType aType = new AnnotatedType().javaClass(genericityContentType).parent(schema)
						.resolveAsRef(annotatedType.isResolveAsRef())
						.jsonViewAnnotation(annotatedType.getJsonViewAnnotation()).skipSchemaName(true)
						.schemaProperty(true).propertyName(targetClass.getName());
				Schema genericSchema = context.resolve(aType);// 解析后的泛型不用增加到字段中去？
			}
		}

		try {
			if (targetClass.isEnum()) {
				for (JavaField enumConstant : targetClass.getEnumConstants()) {
					schema.addEnumItemObject(enumConstant.getComment() + " " + enumConstant.getName());
				}
				schema.setType(PrimitiveType.STRING.getCommonName());
			} else {
				// 转换成OpenApi定义的字段信息
				PrimitiveType parentType = PrimitiveType.fromType(targetClass.getBinaryName());
				schema.setType(Optional.ofNullable(parentType).orElse(PrimitiveType.OBJECT).getCommonName());
				// schema.setType(parentType==null?targetClass.getName():parentType.getCommonName());
			}
		} catch (Exception e) {
			// 转换成OpenApi定义的字段信息
			schema.setType(PrimitiveType.OBJECT.getCommonName());
		}

		if (DocUtils.isPrimitive(targetClass.getName())) {// 基础类型就不用继续往下面解析了，到这里就返回
			if (targetClass.isArray()) {
				Schema array = new Schema();
				array.setType(schema.getType());
				schema = new ArraySchema().items(array);
			}
			return schema;
		}

		// 分析类的字段
		List<JavaField> fields = new ArrayList<>();

		// 循环当前类信息，所有属性都加到fields
		JavaClass cls = targetClass;
		while (cls != null && !cls.isArray() && !"java.lang.Object".equals(cls.getFullyQualifiedName())) {
			fields.addAll(cls.getFields());
			cls = cls.getSuperJavaClass();
		}

		if (targetClass.isA(Map.class.getName())) {
			// 泛型信息
			List<JavaType> tar = new LinkedList<>();
			if (targetClass instanceof DefaultJavaParameterizedType) {
				tar = ((DefaultJavaParameterizedType) targetClass).getActualTypeArguments();
			} else if (targetClass instanceof DefaultJavaClass) {
				tar = ((DefaultJavaClass) targetClass).getImplements();
			}

			if (tar.isEmpty()) {
				return null;
			}
			JavaType javaType = tar.get(1);// 取Value类型做为泛型

			Schema<?> addPropertiesSchema = context
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
			Schema<?> mapSchema = new MapSchema().additionalProperties(addPropertiesSchema);
			mapSchema.name(schema.getName());
			//mapSchema.setType(targetClass.getBinaryName());
			mapSchema.setType(PrimitiveType.OBJECT.getCommonName());
			schema.setType(PrimitiveType.OBJECT.getCommonName());
			mapSchema.addProperties(annotatedType.getPropertyName(), schema);
			mapSchema.setDescription(schema.getDescription());
			schema = mapSchema;
		} else if (targetClass.isA(Collection.class.getName()) || targetClass.isA(List.class.getName())) {

			// 泛型信息
			List<JavaType> tar = new LinkedList<>();
			if (targetClass instanceof DefaultJavaParameterizedType) {// 如果存在泛型信息，此处获取真正的类型信息
				tar = ((DefaultJavaParameterizedType) targetClass).getActualTypeArguments();
			} else if (targetClass instanceof DefaultJavaClass) {
				tar = ((DefaultJavaClass) targetClass).getImplements();
			}
			if (tar.isEmpty()) {
				return null;
			}
			JavaType javaType = tar.get(0);
			// 处理泛型类型，通过递归的方式
			Schema<?> items = context.resolve(new AnnotatedType()
					.javaClass(builder.getClassByName(javaType.getBinaryName()))
					.schemaProperty(annotatedType.isSchemaProperty()).skipSchemaName(true)
					.resolveAsRef(annotatedType.isResolveAsRef()).propertyName(annotatedType.getPropertyName())
					.jsonViewAnnotation(annotatedType.getJsonViewAnnotation()).parent(annotatedType.getParent()));

			if (items == null) {
				return null;
			}

			schema.addProperties(javaType.getBinaryName(), items);
			Schema<?> arraySchema = new ArraySchema().items(items);
			arraySchema.name(schema.getName());
			//Collection要设置为Object类型，否则不能够正确展示
			arraySchema.setType(PrimitiveType.OBJECT.getCommonName());
			schema.setType(PrimitiveType.OBJECT.getCommonName());
			arraySchema.addProperties(annotatedType.getPropertyName(), schema);
			schema = arraySchema;
		}

		Map<String, JavaField> collect = fields.stream()
				.collect(Collectors.toMap(JavaField::getName, r -> r, (r1, r2) -> r1));

		for (BeanPropertyDefinition propertyDef : beanDesc.findProperties()) {
			JavaField field = collect.get(propertyDef.getName());
			if (field == null) {
				continue;
			}

			JavaClass type = field.getType();
			String typeName = findName(genericityContentType == null ? type : genericityContentType);
			if (typeName == null) {
				continue;
			}
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
					|| "R".equals(field.getType().getGenericFullyQualifiedName())
					|| "U".equals(field.getType().getGenericFullyQualifiedName())
					|| field.getType().getGenericFullyQualifiedName().length() == 1
					|| "java.lang.Object".equals(field.getType().getGenericFullyQualifiedName()))) {
				if (DocUtils.isList(genericityContentType.getBinaryName())) {
					aType = new AnnotatedType().javaClass(genericityContentType).parent(schema)
							.resolveAsRef(annotatedType.isResolveAsRef())
							.jsonViewAnnotation(annotatedType.getJsonViewAnnotation()).skipSchemaName(true)
							.schemaProperty(true).propertyName(genericityContentType.getName());
					propSchema = context.resolve(aType);
				} else {
					if (!DocUtils.isPrimitive(findName(genericityContentType))) {
						propSchema.set$ref(constructRef(findName(genericityContentType)));
					}else {
						//泛型内部为基础类型时，如String,Integer的处理
						Schema<?> typeSchema = new Schema();
						PrimitiveType primitiveType = PrimitiveType.fromType(findName(genericityContentType));
						typeSchema.setType(Optional.ofNullable(primitiveType).orElse(PrimitiveType.OBJECT).getCommonName());
						typeSchema.setDescription("类型："+genericityContentType.getBinaryName());
						propSchema.addProperties(genericityContentType.getName(), typeSchema);
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
		if (type == null) {
			return null;
		}
		try {
			stringBuilder.append(type.getName());
		} catch (NullPointerException e) {
			log.debug("获取type:" + type.getClass() + "的名称发生空指针异常！");
			return null;
		}
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