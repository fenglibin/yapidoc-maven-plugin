package com.eeeffff.yapidoc.maven;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.eeeffff.yapidoc.maven.yapi.upload.UploadToYapi;
import com.eeeffff.yapidoc.models.OpenAPI;
import com.eeeffff.yapidoc.utils.Json;

public class UploadTest {
	@Test
	public void testUpload() throws IOException {
		String filePath = "";
		String yapiProjectToken = "";
		String yapiUrl="";
		String json = FileUtils.readFileToString(new File(filePath), "utf-8");
		OpenAPI openAPI = Json.mapper().readValue(json, OpenAPI.class);
		UploadToYapi uploadToYapi = new UploadToYapi(yapiProjectToken, yapiUrl);
		uploadToYapi.upload(openAPI, true);
	}
}
