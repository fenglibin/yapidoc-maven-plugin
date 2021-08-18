package com.eeeffff.yapidoc.maven.ssh;

import org.apache.maven.plugins.annotations.Parameter;

import lombok.Data;

/**
 * @author fenglibin
 * @version V1.0
 */
@Data
public class SCPConfig {
	/**
	 * 目录
	 */
	@Parameter(property = "remoteTargetDirectory", required = true)
	private String remoteTargetDirectory;

	/**
	 * 权限
	 */
	@Parameter(property = "model")
	private String model;
}