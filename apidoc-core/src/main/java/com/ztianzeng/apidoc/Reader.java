package com.ztianzeng.apidoc;

import static com.ztianzeng.apidoc.constants.HtmlRex.HTML_P_PATTERN;
import static com.ztianzeng.apidoc.constants.SpringMvcConstants.REQUEST_BODY_FULLY;
import static com.ztianzeng.apidoc.utils.DocUtils.getRequestMappingMethod;
import static com.ztianzeng.apidoc.utils.DocUtils.getRequestMappingUrl;
import static com.ztianzeng.apidoc.utils.DocUtils.isContentBody;
import static com.ztianzeng.apidoc.utils.DocUtils.isRequestMapping;
import static com.ztianzeng.apidoc.utils.RefUtils.constructRef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.ztianzeng.apidoc.constants.RequestMethod;
import com.ztianzeng.apidoc.models.Components;
import com.ztianzeng.apidoc.models.OpenAPI;
import com.ztianzeng.apidoc.models.Operation;
import com.ztianzeng.apidoc.models.PathItem;
import com.ztianzeng.apidoc.models.Paths;
import com.ztianzeng.apidoc.models.media.ArraySchema;
import com.ztianzeng.apidoc.models.media.Content;
import com.ztianzeng.apidoc.models.media.MediaType;
import com.ztianzeng.apidoc.models.media.Schema;
import com.ztianzeng.apidoc.models.parameters.Parameter;
import com.ztianzeng.apidoc.models.parameters.RequestBody;
import com.ztianzeng.apidoc.models.responses.ApiResponse;
import com.ztianzeng.apidoc.models.responses.ApiResponses;
import com.ztianzeng.apidoc.utils.DocUtils;
import com.ztianzeng.apidoc.utils.Json;
import com.ztianzeng.apidoc.utils.QdoxUtils;

/**
 * open api
 *
 * @author zhaotianzeng
 * @version V1.0
 * @date 2019-06-07 12:01
 */
public class Reader {
	private final OpenAPI openAPI;
	private Paths paths;
	private SourceBuilder sourceBuilder;
	private JavaProjectBuilder builder;
	private Components components;
	private ObjectMapper mapper;

	public Reader() {
		this.openAPI = new OpenAPI();
		this.sourceBuilder = new SourceBuilder();
		paths = new Paths();
		components = new Components();
		this.builder = sourceBuilder.getBuilder();
		mapper = Json.mapper();
	}

	public Reader(OpenAPI openAPI) {
		this.openAPI = openAPI;
		paths = new Paths();
		components = new Components();
		this.sourceBuilder = new SourceBuilder();
		this.builder = sourceBuilder.getBuilder();
		mapper = Json.mapper();
	}

	public Reader(OpenAPI openAPI, SourceBuilder sourceBuilder) {
		this.openAPI = openAPI;
		paths = new Paths();
		components = new Components();
		this.sourceBuilder = sourceBuilder;
		this.builder = sourceBuilder.getBuilder();
		mapper = Json.mapper();
	}

	/**
	 * 读取class的method
	 *
	 * @param classByName
	 * @return
	 */
	public OpenAPI read(JavaClass classByName) {
		openAPI.setPaths(this.paths);
		openAPI.setComponents(components);

		// controller上面的URL
		String classBaseUrl = null;

		for (JavaAnnotation annotation : classByName.getAnnotations()) {
			if (isRequestMapping(annotation)) {
				classBaseUrl = getRequestMappingUrl(annotation);
			}
		}

		// 处理方法
		for (JavaMethod method : classByName.getMethods()) {
			RequestMethod methodType;
			boolean deprecated = !QdoxUtils.getAnnotation(method, Deprecated.class).isPresent();

			;
			List<JavaAnnotation> annotations = method.getAnnotations();

			JavaAnnotation requestMapping = null;
			for (JavaAnnotation annotation : annotations) {
				if (isRequestMapping(annotation)) {
					requestMapping = annotation;
					break;
				}
			}
			if (requestMapping == null) {
				continue;
			}

			String url = getRequestMappingUrl(requestMapping);
			methodType = getRequestMappingMethod(requestMapping);

			if (url != null) {
				url = url.replaceAll("\"", "").trim();
			}
			if (classBaseUrl != null) {
				url = classBaseUrl + url;
			}
			PathItem pathItemObject;
			if (paths != null && paths.get(url) != null) {
				pathItemObject = paths.get(url);
			} else {
				pathItemObject = new PathItem();
			}

			Operation operation = parseMethod(method, deprecated, classByName.getComment());

			setPathItemOperation(pathItemObject, methodType, operation);

			if (StringUtils.isBlank(operation.getOperationId())) {
				operation.setOperationId(getOperationId(method.getName()));
			}

			paths.addPathItem(url, pathItemObject);
		}

		return openAPI;
	}

	public OpenAPI read(Set<JavaClass> classes) {

		for (JavaClass aClass : classes) {
			OpenAPI read = read(aClass);

			paths.putAll(read.getPaths());
			if (components.getSchemas() == null) {
				components.setSchemas(new HashMap<>(20));
			}
			components.getSchemas().putAll(read.getComponents().getSchemas());

		}

		return openAPI;
	}

	protected String getOperationId(String operationId) {
		boolean operationIdUsed = existOperationId(operationId);
		String operationIdToFind = null;
		int counter = 0;
		while (operationIdUsed) {
			operationIdToFind = String.format("%s_%d", operationId, ++counter);
			operationIdUsed = existOperationId(operationIdToFind);
		}
		if (operationIdToFind != null) {
			operationId = operationIdToFind;
		}
		return operationId;
	}

	private boolean existOperationId(String operationId) {
		if (openAPI == null) {
			return false;
		}
		if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
			return false;
		}
		for (PathItem path : openAPI.getPaths().values()) {
			Set<String> pathOperationIds = extractOperationIdFromPathItem(path);
			if (pathOperationIds.contains(operationId)) {
				return true;
			}
		}
		return false;
	}

	private Set<String> extractOperationIdFromPathItem(PathItem path) {
		Set<String> ids = new HashSet<>();
		if (path.getGet() != null && StringUtils.isNotBlank(path.getGet().getOperationId())) {
			ids.add(path.getGet().getOperationId());
		}
		if (path.getPost() != null && StringUtils.isNotBlank(path.getPost().getOperationId())) {
			ids.add(path.getPost().getOperationId());
		}
		if (path.getPut() != null && StringUtils.isNotBlank(path.getPut().getOperationId())) {
			ids.add(path.getPut().getOperationId());
		}
		if (path.getDelete() != null && StringUtils.isNotBlank(path.getDelete().getOperationId())) {
			ids.add(path.getDelete().getOperationId());
		}
		if (path.getOptions() != null && StringUtils.isNotBlank(path.getOptions().getOperationId())) {
			ids.add(path.getOptions().getOperationId());
		}
		if (path.getHead() != null && StringUtils.isNotBlank(path.getHead().getOperationId())) {
			ids.add(path.getHead().getOperationId());
		}
		if (path.getPatch() != null && StringUtils.isNotBlank(path.getPatch().getOperationId())) {
			ids.add(path.getPatch().getOperationId());
		}
		return ids;
	}

	private void setPathItemOperation(PathItem pathItemObject, RequestMethod method, Operation operation) {
		switch (method) {
		case POST:
			pathItemObject.post(operation);
			break;
		case GET:
			pathItemObject.get(operation);
			break;
		case DELETE:
			pathItemObject.delete(operation);
			break;
		case PUT:
			pathItemObject.put(operation);
			break;

		default:
			// Do nothing here
			break;
		}
	}

	/**
	 * 处理方法
	 *
	 * @return
	 */
	public Operation parseMethod(JavaMethod javaMethod, boolean deprecated, String tag) {
		Operation build = Operation.builder().deprecated(deprecated).build();
		setDescAndSummary(build, javaMethod);
		if (StringUtils.isNotBlank(tag)) {
			build.addTagsItem(tag);
		}
		setParametersItem(build, javaMethod);

		setRequestBody(build, javaMethod);

		// 处理方法的信息
		Map<String, Schema> schemaMap = ModelConverters.getInstance().readAll(javaMethod.getReturns());

		ApiResponses responses = new ApiResponses();

		ApiResponse apiResponse = new ApiResponse();

		// 必须得返回一个描述，否则swagger报错
		String aReturn = Optional.ofNullable(javaMethod.getTagByName("return")).map(DocletTag::getValue)
				.orElse("response");
		apiResponse.setDescription(aReturn);

		Content content = new Content();
		MediaType mediaType = new MediaType();

		Schema objectSchema = ModelConverters.getInstance().resolve(javaMethod.getReturns());

		if (objectSchema != null) {
			if (objectSchema instanceof ArraySchema) {
				((ArraySchema) objectSchema).getItems()
						.$ref(constructRef(schemaMap.keySet().stream().findFirst().orElse("")));
			} else {
				objectSchema.$ref(constructRef(objectSchema.getName()));

			}
		}

		mediaType.schema(objectSchema);
		content.addMediaType("application/json", mediaType);
		apiResponse.setContent(content);
		// 成功时候的返回
		responses.addApiResponse("200", apiResponse);

		// swagger规定必须有一个response
		if (responses.size() == 0) {
			apiResponse.setDescription("response");
			responses.addApiResponse("200", apiResponse);
		}

		// 在这边添加schema
		schemaMap.forEach((key, schema) -> {
			components.addSchemas(key, schema);
		});

		build.setResponses(responses);
		return build;
	}

	/**
	 * 设置方法的请求参数
	 */
	private void setParametersItem(Operation apiMethodDoc, JavaMethod method) {
		List<JavaParameter> parameters = method.getParameters();

		Map<String, String> paramDesc = getParamTag(method);

		for (JavaParameter parameter : parameters) {
			if (isContentBody(parameter.getAnnotations())) {
				return;
			}
			boolean required = true;
			String name = parameter.getName();
			for (JavaAnnotation javaAnnotation : parameter.getAnnotations()) {
				String annotationName = javaAnnotation.getType().getFullyQualifiedName();
				if (REQUEST_BODY_FULLY.equals(annotationName)) {
					if (StringUtils.isNotEmpty((String) javaAnnotation.getNamedParameter("name"))) {
						name = (String) javaAnnotation.getNamedParameter("name");
					}
					required = (Boolean) javaAnnotation.getNamedParameter("required");
				}
			}
			// 如果是私有属性直接便利
			if (DocUtils.isPrimitive(parameter.getType().getBinaryName())) {
				Parameter inputParameter = new Parameter();
				inputParameter.in("query");
				inputParameter.setName(name);
				inputParameter.setRequired(required);
				Schema schema = new Schema();

				inputParameter.setDescription(paramDesc.get(parameter.getName()));
				inputParameter.setSchema(schema);
				apiMethodDoc.addParametersItem(inputParameter);
			} else {
				Map<String, Schema> stringSchemaMap = ModelConverters.getInstance().readAll(parameter.getJavaClass());
				for (String s : stringSchemaMap.keySet()) {
					Schema schema = stringSchemaMap.get(s);
					Map<String, Schema> properties = schema.getProperties();
					if (properties == null) {
						continue;
					}
					properties.forEach((k, v) -> {
						Parameter inputParameter = new Parameter();
						inputParameter.setName(k);
						inputParameter.in("query");
						inputParameter.setSchema(v);
						apiMethodDoc.addParametersItem(inputParameter);
					});
				}

			}

		}

	}

	/**
	 * 设置方法的请求参数
	 */
	private void setRequestBody(Operation apiMethodDoc, JavaMethod method) {
		List<JavaParameter> parameters = method.getParameters();

		for (int i = 0; i < parameters.size(); i++) {
			JavaParameter parameter = parameters.get(i);
			if (!isContentBody(parameter.getAnnotations())) {
				return;
			}

			Schema objectSchema = ModelConverters.getInstance().resolve(parameter.getJavaClass());

			Map<String, Schema> schemaMap = ModelConverters.getInstance().readAll(parameter.getJavaClass());

			if (objectSchema instanceof ArraySchema) {
				((ArraySchema) objectSchema).getItems()
						.$ref(constructRef(schemaMap.keySet().stream().findFirst().orElse("")));
			} else {
				objectSchema.$ref(constructRef(objectSchema.getName()));

			}

			RequestBody requestBody = new RequestBody();
			requestBody.setRequired(true);
			Content content = new Content();
			MediaType mediaType = new MediaType();

			mediaType.schema(objectSchema);
			content.addMediaType("application/json", mediaType);
			requestBody.content(content);

			schemaMap.forEach((key, schema) -> {
				components.addSchemas(key, schema);
			});
			apiMethodDoc.setRequestBody(requestBody);
		}

	}

	/**
	 * 设置方法上的详情和概述
	 *
	 * @param apiMethodDoc
	 * @param method
	 */
	private void setDescAndSummary(Operation apiMethodDoc, JavaMethod method) {
		String comment = method.getComment();

		if (comment != null) {
			String desc = null;
			Matcher m = HTML_P_PATTERN.matcher(comment);

			if (m.find()) {
				desc = m.group(0).replace("<p>", "").replace("</p>", "").trim();
				comment = m.replaceAll("");
			}
			apiMethodDoc.setSummary(comment.trim());
			apiMethodDoc.setDescription(desc);
		}

	}

	/**
	 * 获取方法上param注解
	 *
	 * @param javaMethod java方法
	 * @return
	 */
	private Map<String, String> getParamTag(final JavaMethod javaMethod) {
		List<DocletTag> paramTags = javaMethod.getTagsByName("param");
		Map<String, String> paramTagMap = new HashMap<>();
		for (DocletTag docletTag : paramTags) {
			String value = docletTag.getValue();
			String pName;
			String pValue;
			int idx = value.indexOf("\n");
			// 如果存在换行
			if (idx > -1) {
				pName = value.substring(0, idx);
				pValue = value.substring(idx + 1);
			} else {
				pName = (value.contains(" ")) ? value.substring(0, value.indexOf(" ")) : value;
				pValue = value.contains(" ") ? value.substring(value.indexOf(' ') + 1) : "";
			}
			paramTagMap.put(pName.trim(), pValue.trim());
		}
		return paramTagMap;
	}

}