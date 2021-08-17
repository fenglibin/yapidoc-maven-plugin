/**
 * Copyright © 2017 - 2020 Cnabc. All Rights Reserved.
 */

package com.ztianzeng.apidoc.maven;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.collect.Lists;
import com.thoughtworks.qdox.model.JavaClass;
import com.ztianzeng.apidoc.Reader;
import com.ztianzeng.apidoc.SourceBuilder;
import com.ztianzeng.apidoc.maven.ssh.SSHConfig;
import com.ztianzeng.apidoc.maven.ssh.SSHCopy;
import com.ztianzeng.apidoc.maven.yapi.upload.UploadToYapi;
import com.ztianzeng.apidoc.models.OpenAPI;
import com.ztianzeng.apidoc.models.info.Info;
import com.ztianzeng.apidoc.utils.Json;

/**
 * @author tianzeng
 * @goal apidoc
 * @requiresDependencyResolution runtime
 */
@Mojo(name = "openapi", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class DocMojo extends AbstractMojo {
	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject mavenProject;

	/**
	 * YAPI的远程服务器的地址
	 */
	@Parameter(property = "yapiUrl", required = true)
	private String yapiUrl;

	/**
	 * YAPI中对应项目的Token，在项目的设置->Token中可以查看该值
	 */
	@Parameter(property = "yapiProjectToken", required = true)
	private String yapiProjectToken;

	/**
	 * 指定的多个Controller文件的名称，只针对这些Controller进行处理，多用于测试场景，多个以英文逗号分隔，
	 */
	@Parameter(property = "controllers")
	private String controllers;

	/**
	 * 标题
	 */
	@Parameter(property = "title", defaultValue = "doc")
	private String title;

	/**
	 * 版本
	 */
	@Parameter(property = "version", defaultValue = "1.0")
	private String version;

	/**
	 * The output directory into which to copy the resources.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
	private File outputDirectory;

	/**
	 * 输出的文件名称
	 */
	@Parameter(property = "outFileName", defaultValue = "yapidoc.json")
	private String outFileName;

	/**
	 * 输出的文件名称
	 */
	@Parameter(property = "toJar", defaultValue = "false")
	private Boolean toJar;
	/**
	 * 指定scp目标地址
	 */
	@Parameter(property = "ssh")
	private SSHConfig ssh;

	/**
	 *
	 */
	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	protected MavenSession session;
	/**
	 *
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;
	/**
	 * The character encoding scheme to be applied when filtering resources.
	 */
	@Parameter(defaultValue = "${project.build.sourceEncoding}")
	protected String encoding;
	/**
	 *
	 */
	@Component(role = MavenResourcesFiltering.class, hint = "default")
	protected MavenResourcesFiltering mavenResourcesFiltering;

	@Override
	public void execute() {

		if (!mavenProject.getModules().isEmpty()) {
			return;
		}
		try {
			// 加载classloader
			Set<URL> urls = new HashSet<>();
			List<String> elements = mavenProject.getRuntimeClasspathElements();
			for (String element : elements) {
				urls.add(new File(element).toURI().toURL());
			}
			ClassLoader contextClassLoader = URLClassLoader.newInstance(urls.toArray(new URL[0]),
					Thread.currentThread().getContextClassLoader());

			Thread.currentThread().setContextClassLoader(contextClassLoader);

		} catch (DependencyResolutionRequiredException | MalformedURLException e) {
			throw new RuntimeException(e);
		}
		getLog().info("扫描地址 " + mavenProject.getBasedir().getPath());
		execute(mavenProject.getBasedir().getPath());
	}

	private void execute(String url) {

		SourceBuilder sourceBuilder = new SourceBuilder(url);

		Reader reader = new Reader(new OpenAPI(), sourceBuilder);

		Set<JavaClass> controllerData = sourceBuilder.getControllerData();

		final List<String> controllerList = getControllerList();

		if (controllerList != null) {
			controllerData = controllerData.stream().filter(javaClass -> {
				return controllerList.contains(javaClass.getName());
			}).collect(Collectors.toSet());
		}

		OpenAPI open = reader.read(controllerData);
		Info info = new Info();
		info.title(title);
		info.setVersion(version);

		open.setInfo(info);

		try {

			String filePath = outputDirectory.getPath() + "/" + outFileName;
			File file = new File(filePath);
			Json.mapper().writeValue(file, open);

			// 上传到Yapi服务器
			uploadToYapiServer(filePath);

			if (ssh != null) {
				getLog().info("输出到远程目录" + ssh);
				SSHCopy.put(ssh, file);
			}
			if (toJar) {
				getLog().info("打包到jar中");
				List<String> combinedFilters = Collections.emptyList();
				List<Resource> resources = new ArrayList<>();
				Resource resource = new Resource();
				resource.setIncludes(Collections.singletonList(outFileName));
				resource.setDirectory(project.getBuild().getOutputDirectory());
				resources.add(resource);

				MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(resources,
						getOutputDirectory(), project, encoding, combinedFilters, Collections.emptyList(), session);
				mavenResourcesFiltering.filterResources(mavenResourcesExecution);
			}
			// 删除临时文件
			// file.delete();
		} catch (IOException | MavenFilteringException e) {
			getLog().error("YApi插件执行异常:" + e.getMessage(), e);
		}

	}

	public File getOutputDirectory() {
		return outputDirectory;
	}

	private void uploadToYapiServer(String filePath) throws IOException {
		getLog().info("开始将Api信息上传到Yapi服务器：" + yapiUrl);
		String json = FileUtils.readFileToString(new File(filePath), "utf-8");
		OpenAPI openAPI = Json.mapper().readValue(json, OpenAPI.class);
		UploadToYapi uploadToYapi = new UploadToYapi(yapiProjectToken, yapiUrl);
		uploadToYapi.upload(openAPI, true);
		getLog().info("完成Api信息上传.");
	}

	/**
	 * 获取只处理的Controller列表
	 * 
	 * @return
	 */
	public List<String> getControllerList() {
		List<String> controllerList = null;
		if (StringUtils.isNotEmpty(controllers)) {
			controllerList = Lists.newArrayList(controllers.split(","));
		}
		return controllerList;
	}
}