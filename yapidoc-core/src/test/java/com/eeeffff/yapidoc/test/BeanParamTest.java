package com.eeeffff.yapidoc.test;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.thoughtworks.qdox.model.JavaClass;
import com.eeeffff.yapidoc.Reader;
import com.eeeffff.yapidoc.models.OpenAPI;
import com.eeeffff.yapidoc.models.media.ArraySchema;
import com.eeeffff.yapidoc.models.media.Schema;
import com.eeeffff.yapidoc.models.parameters.Parameter;
import com.eeeffff.yapidoc.test.res.MyBeanParamResource;

public class BeanParamTest {

	@Test
	public void shouldSerializeTypeParameter() {
		JavaClass classByName = TestBase.builder.getClassByName(MyBeanParamResource.class.getName());
		OpenAPI openApi = new Reader(new OpenAPI()).read(classByName);
		List<Parameter> getOperationParams = openApi.getPaths().get("/").getGet().getParameters();
		Assert.assertEquals(getOperationParams.size(), 1);
		Parameter param = getOperationParams.get(0);
		Assert.assertEquals(param.getName(), "listOfStrings");
		Schema<?> schema = param.getSchema();
		// These are the important checks:
		Assert.assertEquals(schema.getClass(), ArraySchema.class);
		Assert.assertEquals(((ArraySchema) schema).getItems().getType(), "string");
	}

}