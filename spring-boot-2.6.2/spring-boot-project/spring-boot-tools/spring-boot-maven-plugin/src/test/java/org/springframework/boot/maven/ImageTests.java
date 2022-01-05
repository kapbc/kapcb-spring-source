/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.maven;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.BuildpackReference;
import org.springframework.boot.buildpack.platform.build.Cache;
import org.springframework.boot.buildpack.platform.build.PullPolicy;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.buildpack.platform.io.Owner;
import org.springframework.boot.buildpack.platform.io.TarArchive;
import org.springframework.boot.maven.CacheInfo.VolumeCacheInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link Image}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 * @author Rafael Ceccone
 */
class ImageTests {

	@Test
	void getBuildRequestWhenNameIsNullDeducesName() {
		BuildRequest request = new Image().getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getName().toString()).isEqualTo("docker.io/library/my-app:0.0.1-SNAPSHOT");
	}

	@Test
	void getBuildRequestWhenNameIsSetUsesName() {
		Image image = new Image();
		image.name = "demo";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getName().toString()).isEqualTo("docker.io/library/demo:latest");
	}

	@Test
	void getBuildRequestWhenNoCustomizationsUsesDefaults() {
		BuildRequest request = new Image().getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getName().toString()).isEqualTo("docker.io/library/my-app:0.0.1-SNAPSHOT");
		assertThat(request.getBuilder().toString()).contains("paketobuildpacks/builder");
		assertThat(request.getRunImage()).isNull();
		assertThat(request.getEnv()).isEmpty();
		assertThat(request.isCleanCache()).isFalse();
		assertThat(request.isVerboseLogging()).isFalse();
		assertThat(request.getPullPolicy()).isEqualTo(PullPolicy.ALWAYS);
		assertThat(request.getBuildpacks()).isEmpty();
		assertThat(request.getBindings()).isEmpty();
		assertThat(request.getNetwork()).isNull();
	}

	@Test
	void getBuildRequestWhenHasBuilderUsesBuilder() {
		Image image = new Image();
		image.builder = "springboot/builder:2.2.x";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuilder().toString()).isEqualTo("docker.io/springboot/builder:2.2.x");
	}

	@Test
	void getBuildRequestWhenHasRunImageUsesRunImage() {
		Image image = new Image();
		image.runImage = "springboot/run:latest";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getRunImage().toString()).isEqualTo("docker.io/springboot/run:latest");
	}

	@Test
	void getBuildRequestWhenHasEnvUsesEnv() {
		Image image = new Image();
		image.env = Collections.singletonMap("test", "test");
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getEnv()).containsExactly(entry("test", "test"));
	}

	@Test
	void getBuildRequestWhenHasCleanCacheUsesCleanCache() {
		Image image = new Image();
		image.cleanCache = true;
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.isCleanCache()).isTrue();
	}

	@Test
	void getBuildRequestWhenHasVerboseLoggingUsesVerboseLogging() {
		Image image = new Image();
		image.verboseLogging = true;
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.isVerboseLogging()).isTrue();
	}

	@Test
	void getBuildRequestWhenHasPullPolicyUsesPullPolicy() {
		Image image = new Image();
		image.setPullPolicy(PullPolicy.NEVER);
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getPullPolicy()).isEqualTo(PullPolicy.NEVER);
	}

	@Test
	void getBuildRequestWhenHasPublishUsesPublish() {
		Image image = new Image();
		image.publish = true;
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.isPublish()).isTrue();
	}

	@Test
	void getBuildRequestWhenHasBuildpacksUsesBuildpacks() {
		Image image = new Image();
		image.buildpacks = Arrays.asList("example/buildpack1@0.0.1", "example/buildpack2@0.0.2");
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuildpacks()).containsExactly(BuildpackReference.of("example/buildpack1@0.0.1"),
				BuildpackReference.of("example/buildpack2@0.0.2"));
	}

	@Test
	void getBuildRequestWhenHasBindingsUsesBindings() {
		Image image = new Image();
		image.bindings = Arrays.asList("host-src:container-dest:ro", "volume-name:container-dest:rw");
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBindings()).containsExactly(Binding.of("host-src:container-dest:ro"),
				Binding.of("volume-name:container-dest:rw"));
	}

	@Test
	void getBuildRequestWhenNetworkUsesNetwork() {
		Image image = new Image();
		image.network = "test";
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getNetwork()).isEqualTo("test");
	}

	@Test
	void getBuildRequestWhenHasTagsUsesTags() {
		Image image = new Image();
		image.tags = Arrays.asList("my-app:latest", "example.com/my-app:0.0.1-SNAPSHOT", "example.com/my-app:latest");
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getTags()).containsExactly(ImageReference.of("my-app:latest"),
				ImageReference.of("example.com/my-app:0.0.1-SNAPSHOT"), ImageReference.of("example.com/my-app:latest"));
	}

	@Test
	void getBuildRequestWhenHasBuildVolumeCacheUsesCache() {
		Image image = new Image();
		image.buildCache = new CacheInfo(new VolumeCacheInfo("build-cache-vol"));
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getBuildCache()).isEqualTo(Cache.volume("build-cache-vol"));
	}

	@Test
	void getBuildRequestWhenHasLaunchVolumeCacheUsesCache() {
		Image image = new Image();
		image.launchCache = new CacheInfo(new VolumeCacheInfo("launch-cache-vol"));
		BuildRequest request = image.getBuildRequest(createArtifact(), mockApplicationContent());
		assertThat(request.getLaunchCache()).isEqualTo(Cache.volume("launch-cache-vol"));
	}

	private Artifact createArtifact() {
		return new DefaultArtifact("com.example", "my-app", VersionRange.createFromVersion("0.0.1-SNAPSHOT"), "compile",
				"jar", null, new DefaultArtifactHandler());
	}

	private Function<Owner, TarArchive> mockApplicationContent() {
		return (owner) -> null;
	}

}
