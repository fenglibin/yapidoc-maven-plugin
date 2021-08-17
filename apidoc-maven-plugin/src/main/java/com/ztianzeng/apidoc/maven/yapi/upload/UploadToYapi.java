package com.ztianzeng.apidoc.maven.yapi.upload;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ztianzeng.apidoc.maven.yapi.constant.YapiConstant;
import com.ztianzeng.apidoc.maven.yapi.utils.HttpClientUtil;
import com.ztianzeng.apidoc.utils.Json;

/**
 * 上传到yapi
 *
 * @author zhaotianzeng
 * @version V1.0
 * @date 2019-06-14 14:18
 */
public class UploadToYapi {
	public static Map<String, Map<String, Integer>> catMap = new HashMap<>();
	public final ObjectMapper mapper = Json.mapper();
	private final String projectToken;
	private final String yapiUrl;

	public UploadToYapi(String projectToken, String yapiUrl) {
		this.projectToken = projectToken;
		this.yapiUrl = yapiUrl;
	}

	/**
	 * 上传OpenAPi
	 *
	 * @param openAPI
	 */
	public void upload(Object openAPI, boolean merge) throws IOException {
		Map<String, Object> send = new HashMap<>(4);
		send.put("type", "swagger");
		send.put("token", projectToken);
		send.put("merge", merge);
		send.put("json", mapper.writeValueAsString(openAPI));

		HttpClientUtil.getHttpclient()
				.execute(getHttpPost(yapiUrl + YapiConstant.IMPUT, mapper.writeValueAsString(send)));
	}

	/**
	 * 获得httpPost
	 *
	 * @return
	 */
	private HttpPost getHttpPost(String url, String body) {
		HttpPost httpPost = null;
		try {
			httpPost = new HttpPost(url);
			httpPost.setHeader("Content-type", "application/json;charset=utf-8");
			HttpEntity reqEntity = new StringEntity(body == null ? "" : body, "UTF-8");
			httpPost.setEntity(reqEntity);
			//设置超时时间
			RequestConfig requestConfig = RequestConfig.custom()  
			        .setConnectTimeout(5000).setConnectionRequestTimeout(5000)  
			        .setSocketTimeout(120000).build(); 
			httpPost.setConfig(requestConfig);
		} catch (Exception e) {
		}
		return httpPost;
	}

}
