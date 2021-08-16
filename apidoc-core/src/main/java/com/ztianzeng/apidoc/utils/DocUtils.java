package com.ztianzeng.apidoc.utils;

import static com.ztianzeng.apidoc.constants.SpringMvcConstants.METHOD_MAP;
import static com.ztianzeng.apidoc.constants.SpringMvcConstants.REQUEST_BODY_ALL;
import static com.ztianzeng.apidoc.constants.SpringMvcConstants.REQUEST_MAPPING;
import static com.ztianzeng.apidoc.constants.SpringMvcConstants.REQUEST_MAPPING_FULLY;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.impl.DefaultJavaParameterizedType;
import com.ztianzeng.apidoc.constants.RequestMethod;

/**
 * 基本工具类
 *
 * @author zhaotianzeng
 * @version V1.0
 * @date 2019-06-05 15:13
 */
public final class DocUtils {
	private DocUtils() {
	}

	/**
	 * 是否为私有属性
	 *
	 * @param type 类型
	 * @return true false
	 */
	public static boolean isPrimitive(String type) {
		type = type.contains("java.lang") ? type.substring(type.lastIndexOf(".") + 1) : type;
		type = type.toLowerCase();
		switch (type) {
		case "integer":
		case "int":
		case "long":
		case "double":
		case "float":
		case "short":
		case "bigdecimal":
		case "char":
		case "string":
		case "boolean":
		case "byte":
		case "java.sql.timestamp":
		case "java.util.date":
		case "java.time.localdatetime":
		case "localdatetime":
		case "localdate":
		case "java.time.localdate":
		case "java.math.bigdecimal":
		case "java.math.biginteger":
			return true;
		default:
			return false;
		}
	}

	/**
	 * 是否为Spring mvc的内置对象
	 *
	 * @param paramType 类型
	 * @return true false
	 */
	public static boolean isMvcParams(String paramType) {
		switch (paramType) {
		case "org.springframework.ui.Model":
		case "org.springframework.ui.ModelMap":
		case "org.springframework.web.servlet.ModelAndView":
		case "org.springframework.validation.BindingResult":
		case "javax.servlet.http.HttpServletRequest":
		case "javax.servlet.http.HttpServletResponse":
			return true;
		default:
			return false;
		}
	}

	/**
	 * 判断属性是否是必须
	 * <p>
	 * 是否有javax.validation中的注解
	 *
	 * @param field 属性
	 */
	public static boolean isRequired(JavaField field) {
		boolean isRequired = false;
		List<JavaAnnotation> annotations = field.getAnnotations();
		for (JavaAnnotation annotation : annotations) {
			String fullyQualifiedName = annotation.getType().getFullyQualifiedName();
			if (fullyQualifiedName.startsWith("javax.validation")) {
				isRequired = true;
			}
		}
		return isRequired;
	}

	/**
	 * 用过名字获取所在的类型
	 *
	 * @param typeName 类型名称
	 * @return javaType
	 */
	public static Type getTypeForName(String typeName) {
		try {
			return TypeFactory.defaultInstance().findClass(typeName);
		} catch (ClassNotFoundException e) {
			try {
				return Class.forName(typeName);
			} catch (ClassNotFoundException ex) {
				return null;
			}
		}

	}

	/**
	 * 是否为@RequestMapping的注解
	 *
	 * @param annotation
	 * @return
	 */
	public static boolean isRequestMapping(JavaAnnotation annotation) {
		String annotationName = annotation.getType().getName();
		return REQUEST_MAPPING.equals(annotationName) || REQUEST_MAPPING_FULLY.equals(annotationName)
				|| METHOD_MAP.get(annotationName) != null;
	}

	/**
	 * 获取URL
	 *
	 * @param annotation
	 * @return
	 */
	public static String getRequestMappingUrl(JavaAnnotation annotation) {
		String baseUrl = "/";
		if (isRequestMapping(annotation)) {
			if (isRequestMapping(annotation)) {
				if (annotation.getNamedParameter("value") == null) {
					return baseUrl;
				}
				baseUrl = annotation.getNamedParameter("value").toString();
				baseUrl = baseUrl.replaceAll("\"", "");
			}
		}
		return baseUrl;
	}

	/**
	 * 获取请求的方法
	 *
	 * @param annotation 注解
	 * @return 请求方法
	 */
	public static RequestMethod getRequestMappingMethod(JavaAnnotation annotation) {
		String methodType;
		String annotationName = annotation.getType().getName();
		if (METHOD_MAP.get(annotationName) != null) {
			return METHOD_MAP.get(annotationName);
		}
		if (null != annotation.getNamedParameter("method")) {
			methodType = annotation.getNamedParameter("method").toString();
			if ("RequestMethod.POST".equals(methodType)) {
				methodType = "POST";
			} else if ("RequestMethod.GET".equals(methodType)) {
				methodType = "GET";
			} else if ("RequestMethod.PUT".equals(methodType)) {
				methodType = "PUT";
			} else if ("RequestMethod.DELETE".equals(methodType)) {
				methodType = "DELETE";
			} else {
				methodType = "GET";
			}
		} else {
			methodType = "GET";
		}
		return RequestMethod.valueOf(methodType);
	}

	/**
	 * 是否使用@requestBody注解
	 *
	 * @return
	 */
	public static boolean isContentBody(List<JavaAnnotation> annotation) {
		for (JavaAnnotation javaAnnotation : annotation) {
			String annotationName = javaAnnotation.getType().getFullyQualifiedName();
			if (REQUEST_BODY_ALL.equals(annotationName)) {
				return true;
			}
		}

		return false;
	}

	public static String findTypeName(JavaType type, BeanDescription beanDesc) {
		ObjectMapper mapper = Json.mapper();
		// First, handle container types; they require recursion
		if (type.isArrayType()) {
			return "Array";
		}

		if (type.isMapLikeType() && ReflectionUtils.isSystemType(type)) {
			return "Map";
		}

		if (type.isContainerType() && ReflectionUtils.isSystemType(type)) {
			if (Set.class.isAssignableFrom(type.getRawClass())) {
				return "Set";
			}
			return "List";
		}
		if (beanDesc == null) {
			beanDesc = mapper.getSerializationConfig().introspectClassAnnotations(type);
		}

		PropertyName rootName = mapper.getSerializationConfig().getAnnotationIntrospector()
				.findRootName(beanDesc.getClassInfo());
		if (rootName != null && rootName.hasSimpleName()) {
			return rootName.getSimpleName();
		}
		StringBuilder stringBuilder = new StringBuilder();
		for (JavaType typeParameter : type.getBindings().getTypeParameters()) {
			stringBuilder.append(typeParameter.getRawClass().getSimpleName());
		}
		return type.getRawClass().getSimpleName() + stringBuilder.toString();
	}

	/**
	 * 获取泛型的数量
	 *
	 * @param returnType
	 * @return
	 */
	public static int genericityCount(JavaClass returnType) {
		if (returnType instanceof DefaultJavaParameterizedType) {
			return ((DefaultJavaParameterizedType) returnType).getActualTypeArguments().size();
		}
		return 0;
	}

	/**
	 * 获取泛型的数量
	 *
	 * @param returnType
	 * @return
	 */
	public static JavaClass genericityContentType(JavaClass returnType) {
		if (returnType instanceof DefaultJavaParameterizedType) {
			if (!((DefaultJavaParameterizedType) returnType).getActualTypeArguments().isEmpty()) {
				return (DefaultJavaParameterizedType) ((DefaultJavaParameterizedType) returnType)
						.getActualTypeArguments().get(0);
			}
		}
		return null;
	}

	/**
	 * 是否为map
	 *
	 * @param type java type
	 * @return boolean
	 */
	public static boolean isMap(String type) {
		switch (type) {
		case "java.util.Map":
		case "java.util.SortedMap":
		case "java.util.TreeMap":
		case "java.util.LinkedHashMap":
		case "java.util.HashMap":
		case "java.util.concurrent.ConcurrentHashMap":
		case "java.util.Properties":
		case "java.util.Hashtable":
			return true;
		default:
			return false;
		}
	}

	public static boolean isList(String type) {
		switch (type) {
		case "java.util.List":
		case "java.util.Collection":
			return true;
		default:
			return false;
		}
	}

	/**
	 * Automatic repair of generic split class names
	 *
	 * @param arr arr of class name
	 * @return array of String
	 */
	private static String[] classNameFix(String[] arr) {
		List<String> classes = new ArrayList<>();
		List<Integer> indexList = new ArrayList<>();
		int globIndex = 0;
		for (int i = 0; i < arr.length; i++) {
			if (!classes.isEmpty()) {
				int index = classes.size() - 1;
				if (!isClassName(classes.get(index))) {
					globIndex = globIndex + 1;
					if (globIndex < arr.length) {
						indexList.add(globIndex);
						String className = classes.get(index) + "," + arr[globIndex];
						classes.set(index, className);
					}

				} else {
					globIndex = globIndex + 1;
					if (globIndex < arr.length) {
						if (isClassName(arr[globIndex])) {
							indexList.add(globIndex);
							classes.add(arr[globIndex]);
						} else {
							if (!indexList.contains(globIndex) && !indexList.contains(globIndex + 1)) {
								indexList.add(globIndex);
								classes.add(arr[globIndex] + "," + arr[globIndex + 1]);
								globIndex = globIndex + 1;
								indexList.add(globIndex);
							}
						}
					}
				}
			} else {
				if (isClassName(arr[i])) {
					indexList.add(i);
					classes.add(arr[i]);
				} else {
					if (!indexList.contains(i) && !indexList.contains(i + 1)) {
						globIndex = i + 1;
						classes.add(arr[i] + "," + arr[globIndex]);
						indexList.add(i);
						indexList.add(i + 1);
					}
				}
			}
		}
		return classes.toArray(new String[classes.size()]);
	}

	/**
	 * 是否是合法的java类名称
	 *
	 * @param className class nem
	 * @return boolean
	 */
	public static boolean isClassName(String className) {
		if (StringUtils.isEmpty(className)) {
			return false;
		}
		if (className.contains("<") && !className.contains(">")) {
			return false;
		} else if (className.contains(">") && !className.contains("<")) {
			return false;
		} else {
			return true;
		}
	}

}