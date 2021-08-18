package com.eeeffff.yapidoc.maven.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SCPOutputStream;

/**
 * ssh 复制工具类
 *
 * @author fenglibin
 * @version V1.0
 */
public final class SSHCopy {
	private SSHCopy() {
	}

	public static void put(SSHConfig connection, File file) throws IOException {
		if (StringUtils.isBlank(connection.getHost())) {
			return;
		}
		String hostname = connection.getHost();
		Connection conn = new Connection(hostname, 22);
		conn.connect();
		boolean auth;
		if (connection.getPublicKey() != null) {
			auth = conn.authenticateWithPublicKey(connection.getUser(), connection.getPublicKey().toCharArray(),
					connection.getPassword());
		} else if (connection.getPublicKeyFile() != null) {
			auth = conn.authenticateWithPublicKey(connection.getUser(), new File(connection.getPublicKeyFile()),
					connection.getPassword());
		} else if (connection.getPassword() != null) {
			auth = conn.authenticateWithPassword(connection.getUser(), connection.getPassword());
		} else {
			auth = conn.authenticateWithNone(connection.getUser());
		}
		if (!auth) {
			throw new IOException("Authentication failed.");
		}

		if (connection.getScp() != null) {
			SCPClient scpClient = conn.createSCPClient();
			scpClient.setCharset("utf-8");
			SCPOutputStream scpOutputStream = scpClient.put(file.getName(), file.length(),
					connection.getScp().getRemoteTargetDirectory(), connection.getScp().getModel());
			String content = IOUtils.toString(new FileInputStream(file), Charset.defaultCharset());
			scpOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
			scpOutputStream.flush();
			scpOutputStream.close();

		}

	}
}