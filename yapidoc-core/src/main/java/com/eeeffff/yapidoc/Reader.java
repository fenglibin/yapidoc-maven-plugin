package com.eeeffff.yapidoc;

import static com.eeeffff.yapidoc.constants.HtmlRex.HTML_P_PATTERN;
import static com.eeeffff.yapidoc.constants.SpringMvcConstants.REQUEST_BODY_FULLY;
import static com.eeeffff.yapidoc.utils.DocUtils.getRequestMappingMethod;
import static com.eeeffff.yapidoc.utils.DocUtils.getRequestMappingUrl;
import static com.eeeffff.yapidoc.utils.DocUtils.isContentBody;
import static com.eeeffff.yapidoc.utils.DocUtils.isRequestMapping;
import static com.eeeffff.yapidoc.utils.RefUtils.constructRef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaParameter;
import com.eeeffff.yapidoc.constants.RequestMethod;
import com.eeeffff.yapidoc.models.Components;
import com.eeeffff.yapidoc.models.OpenAPI;
import com.eeeffff.yapidoc.models.Operation;
import com.eeeffff.yapidoc.models.PathItem;
import com.eeeffff.yapidoc.models.Paths;
import com.eeeffff.yapidoc.models.media.ArraySchema;
import com.eeeffff.yapidoc.models.media.Content;
import com.eeeffff.yapidoc.models.media.MediaType;
import com.eeeffff.yapidoc.models.media.Schema;
import com.eeeffff.yapidoc.models.parameters.Parameter;
import com.eeeffff.yapidoc.models.parameters.RequestBody;
import com.eeeffff.yapidoc.models.responses.ApiResponse;
import com.eeeffff.yapidoc.models.responses.ApiResponses;
import com.eeeffff.yapidoc.utils.DocUtils;
import com.eeeffff.yapidoc.utils.Json;
import com.eeeffff.yapidoc.utils.QdoxUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * open api
 *
 * 
 * @version V1.0
 * @date 2019-06-07 12:01
 */
@Slf4j
public class Reader {
	private final OpenAPI openAPI;
	private Paths paths;
	private SourceBuilder sourceBuilder;
	private JavaProjectBuilder builder;
	private Components components;
	private ObjectMapper mapper;

	public Reader() {
		this.openAPI = new OpenAPI();
		this.sourceBuilder = SourceBuilder.INSTANCE;
		paths = new Paths();
		components = new Components();
		this.builder = sourceBuilder.getBuilder();
		mapper = Json.mapper();
	}

	public Reader(OpenAPI openAPI) {
		this.openAPI = openAPI;
		paths = new Paths();
		components = new Components();
		this.sourceBuilder = SourceBuilder.INSTANCE;
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
	 * ??????class???method
	 *
	 * @param classByName
	 * @return
	 */
	public OpenAPI read(JavaClass classByName) {
		openAPI.setPaths(this.paths);
		openAPI.setComponents(components);

		// controller?????????URL
		String classBaseUrl = null;

		for (JavaAnnotation annotation : classByName.getAnnotations()) {
			if (isRequestMapping(annotation)) {
				classBaseUrl = getRequestMappingUrl(annotation);
			}
		}

		// ????????????
		for (JavaMethod method : classByName.getMethods()) {
			RequestMethod methodType;
			boolean deprecated = !QdoxUtils.getAnnotation(method, Deprecated.class).isPresent();

			List<JavaAnnotation> annotations = method.getAnnotations();

			JavaAnnotation requestMapping = null;
			for (JavaAnnotation annotation : annotations) {
				if (isRequestMapping(annotation)) {
					requestMapping = annotation;
					break;
				}
			}
			// ??????????????????????????????????????????????????????
			if (requestMapping == null) {
				continue;
			}

			String url = getRequestMappingUrl(requestMapping);
			methodType = getRequestMappingMethod(requestMapping);

			// ????????????????????????????????????
			if (url != null) {
				url = url.replaceAll("\"", "").trim();
			}
			// ???????????????????????????????????????????????????????????????????????????????????????
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

	/**
	 * ???Controller????????????
	 * 
	 * @param controllerClasses
	 * @return
	 */
	public OpenAPI read(Set<JavaClass> controllerClasses) {

		for (JavaClass aClass : controllerClasses) {
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
	 * ???????????????????????????????????????????????????????????????????????????
	 *
	 * @return
	 */
	public Operation parseMethod(JavaMethod javaMethod, boolean deprecated, String tag) {
		Operation operation = Operation.builder().deprecated(deprecated).build();

		// ?????????????????????????????????
		setDescAndSummary(operation, javaMethod);
		if (StringUtils.isNotBlank(tag)) {
			operation.addTagsItem(tag);
		}
		// ?????????????????????????????????
		setParametersItem(operation, javaMethod);

		// ????????????????????????????????????????????????????????????@RequestBody???????????????
		setRequestBody(operation, javaMethod);

		// ???????????????????????????
		Map<String, Schema> schemaMap = ModelConverters.getInstance().readAll(javaMethod.getReturns(),
				javaMethod.getName());

		ApiResponses responses = new ApiResponses();

		ApiResponse apiResponse = new ApiResponse();

		// ????????????????????????????????????swagger??????
		String aReturn = Optional.ofNullable(javaMethod.getTagByName("return")).map(DocletTag::getValue)
				.orElse("response");
		apiResponse.setDescription(aReturn);

		Content content = new Content();
		MediaType mediaType = new MediaType();

		Schema objectSchema = ModelConverters.getInstance().resolve(javaMethod.getReturns(), javaMethod.getName());

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
		// ?????????????????????
		responses.addApiResponse("200", apiResponse);

		// swagger?????????????????????response
		if (responses.size() == 0) {
			apiResponse.setDescription("response");
			responses.addApiResponse("200", apiResponse);
		}

		// ???????????????schema
		schemaMap.forEach((key, schema) -> {
			components.addSchemas(key, schema);
		});

		operation.setResponses(responses);
		return operation;
	}

	/**
	 * ?????????????????????????????????
	 */
	private void setParametersItem(Operation operation, JavaMethod method) {
		List<JavaParameter> parameters = method.getParameters();

		// ???????????????????????????????????????
		Map<String, String> paramDesc = getParamTag(method);

		for (JavaParameter parameter : parameters) {
			if (isContentBody(parameter.getAnnotations())) {
				continue;
			}
			AtomicBoolean requiredBoolean = new AtomicBoolean(true);

			String name = parameter.getName();
			for (JavaAnnotation javaAnnotation : parameter.getAnnotations()) {
				String annotationName = javaAnnotation.getType().getFullyQualifiedName();
				if (REQUEST_BODY_FULLY.equals(annotationName)) {
					if (StringUtils.isNotEmpty((String) javaAnnotation.getNamedParameter("name"))) {
						name = (String) javaAnnotation.getNamedParameter("name");
					}
					requiredBoolean.set((Boolean) javaAnnotation.getNamedParameter("required"));
				}
			}
			// ?????????????????????????????????
			if (DocUtils.isPrimitive(parameter.getType().getBinaryName())) {
				Parameter inputParameter = new Parameter();
				inputParameter.in("query");
				inputParameter.setName(name);
				inputParameter.setRequired(requiredBoolean.get());
				//inputParameter.setType(parameter.getType().getBinaryName());
				Schema schema = new Schema();
				String desc = StringUtils.isEmpty(paramDesc.get(parameter.getName())) ? ""
						: paramDesc.get(parameter.getName()) + "???";
				inputParameter.setDescription(desc + "???????????????" + parameter.getType().getCanonicalName());
				inputParameter.setSchema(schema);
				operation.addParametersItem(inputParameter);
			} else {
				Map<String, Schema> stringSchemaMap = ModelConverters.getInstance().readAll(parameter.getJavaClass(),
						parameter.getName());
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
						//inputParameter.setType(parameter.getType().getBinaryName());
						//v.setType(parameter.getType().getCanonicalName());
						inputParameter.setRequired(requiredBoolean.get());
						String desc = StringUtils.isEmpty(paramDesc.get(parameter.getName())) ? ""
								: paramDesc.get(parameter.getName()) + "???";
						inputParameter.setDescription(desc + v.getDescription());
						operation.addParametersItem(inputParameter);
					});

				}

			}

		}

	}

	/**
	 * ????????????????????????????????????????????????????????????@RequestBody???????????????
	 */
	private void setRequestBody(Operation operation, JavaMethod method) {
		List<JavaParameter> parameters = method.getParameters();

		for (int i = 0; i < parameters.size(); i++) {
			JavaParameter parameter = parameters.get(i);
			if (!isContentBody(parameter.getAnnotations())) {
				continue;
			}

			Schema objectSchema = ModelConverters.getInstance().resolve(parameter.getJavaClass(), parameter.getName());
			if (objectSchema == null) {
				return;
			}

			Map<String, Schema> schemaMap = ModelConverters.getInstance().readAll(parameter.getJavaClass(),
					parameter.getName());

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
				if (key != null) {
					components.addSchemas(key, schema);
				}
			});
			operation.setRequestBody(requestBody);
		}

	}

	/**
	 * ?????????????????????????????????
	 *
	 * @param operation
	 * @param method
	 */
	private void setDescAndSummary(Operation operation, JavaMethod method) {
		String comment = method.getComment();

		if (comment != null) {
			String desc = null;
			Matcher m = HTML_P_PATTERN.matcher(comment);

			if (m.find()) {
				desc = m.group(0).replace("<p>", "").replace("</p>", "").trim();
				comment = m.replaceAll("");
			}
			operation.setSummary(comment.trim());
			operation.setDescription(desc);
		}

	}

	/**
	 * ???????????????param??????
	 *
	 * @param javaMethod java??????
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
			// ??????????????????
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