/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.buildpack.platform.docker.transport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpTransport}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class HttpTransportTests {

	@Test
	void createWhenDockerHostVariableIsAddressReturnsRemote() {
		Map<String, String> environment = Collections.singletonMap("DOCKER_HOST", "tcp://192.168.1.0");
		HttpTransport transport = HttpTransport.create(environment::get);
		assertThat(transport).isInstanceOf(RemoteHttpClientTransport.class);
	}

	@Test
	void createWhenDockerHostVariableIsFileReturnsLocal(@TempDir Path tempDir) throws IOException {
		String dummySocketFilePath = Files.createTempFile(tempDir, "http-transport", null).toAbsolutePath().toString();
		Map<String, String> environment = Collections.singletonMap("DOCKER_HOST", dummySocketFilePath);
		HttpTransport transport = HttpTransport.create(environment::get);
		assertThat(transport).isInstanceOf(LocalHttpClientTransport.class);
	}

	@Test
	void createWhenDockerHostVariableIsUnixSchemePrefixedFileReturnsLocal(@TempDir Path tempDir) throws IOException {
		String dummySocketFilePath = "unix://"
				+ Files.createTempFile(tempDir, "http-transport", null).toAbsolutePath().toString();
		Map<String, String> environment = Collections.singletonMap("DOCKER_HOST", dummySocketFilePath);
		HttpTransport transport = HttpTransport.create(environment::get);
		assertThat(transport).isInstanceOf(LocalHttpClientTransport.class);
	}

	@Test
	void createWhenDoesNotHaveDockerHostVariableReturnsLocal() {
		HttpTransport transport = HttpTransport.create((name) -> null);
		assertThat(transport).isInstanceOf(LocalHttpClientTransport.class);
	}

}
