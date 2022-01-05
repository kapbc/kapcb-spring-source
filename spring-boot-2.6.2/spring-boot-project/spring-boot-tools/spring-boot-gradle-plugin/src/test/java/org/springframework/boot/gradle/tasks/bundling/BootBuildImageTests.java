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

package org.springframework.boot.gradle.tasks.bundling;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.buildpack.platform.build.BuildRequest;
import org.springframework.boot.buildpack.platform.build.BuildpackReference;
import org.springframework.boot.buildpack.platform.build.PullPolicy;
import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.ImageReference;
import org.springframework.boot.gradle.junit.GradleProjectBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link BootBuildImage}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @author Jeroen Meijer
 * @author Rafael Ceccone
 */
class BootBuildImageTests {

	Project project;

	private BootBuildImage buildImage;

	@BeforeEach
	void setUp(@TempDir File temp) {
		File projectDir = new File(temp, "project");
		projectDir.mkdirs();
		this.project = GradleProjectBuilder.builder().withProjectDir(projectDir).withName("build-image-test").build();
		this.project.setDescription("Test project for BootBuildImage");
		this.buildImage = this.project.getTasks().create("buildImage", BootBuildImage.class);
	}

	@Test
	void whenProjectVersionIsUnspecifiedThenItIsIgnoredWhenDerivingImageName() {
		assertThat(this.buildImage.getImageName()).isEqualTo("docker.io/library/build-image-test");
		BuildRequest request = this.buildImage.createRequest();
		assertThat(request.getName().getDomain()).isEqualTo("docker.io");
		assertThat(request.getName().getName()).isEqualTo("library/build-image-test");
		assertThat(request.getName().getTag()).isEqualTo("latest");
		assertThat(request.getName().getDigest()).isNull();
	}

	@Test
	void whenProjectVersionIsSpecifiedThenItIsUsedInTagOfImageName() {
		this.project.setVersion("1.2.3");
		assertThat(this.buildImage.getImageName()).isEqualTo("docker.io/library/build-image-test:1.2.3");
		BuildRequest request = this.buildImage.createRequest();
		assertThat(request.getName().getDomain()).isEqualTo("docker.io");
		assertThat(request.getName().getName()).isEqualTo("library/build-image-test");
		assertThat(request.getName().getTag()).isEqualTo("1.2.3");
		assertThat(request.getName().getDigest()).isNull();
	}

	@Test
	void whenImageNameIsSpecifiedThenItIsUsedInRequest() {
		this.project.setVersion("1.2.3");
		this.buildImage.setImageName("example.com/test/build-image:1.0");
		assertThat(this.buildImage.getImageName()).isEqualTo("example.com/test/build-image:1.0");
		BuildRequest request = this.buildImage.createRequest();
		assertThat(request.getName().getDomain()).isEqualTo("example.com");
		assertThat(request.getName().getName()).isEqualTo("test/build-image");
		assertThat(request.getName().getTag()).isEqualTo("1.0");
		assertThat(request.getName().getDigest()).isNull();
	}

	@Test
	void springBootVersionDefaultValueIsUsed() {
		BuildRequest request = this.buildImage.createRequest();
		assertThat(request.getCreator().getName()).isEqualTo("Spring Boot");
		assertThat(request.getCreator().getVersion()).isEqualTo("");
	}

	@Test
	void whenIndividualEntriesAreAddedToTheEnvironmentThenTheyAreIncludedInTheRequest() {
		this.buildImage.environment("ALPHA", "a");
		this.buildImage.environment("BRAVO", "b");
		assertThat(this.buildImage.createRequest().getEnv()).containsEntry("ALPHA", "a").containsEntry("BRAVO", "b")
				.hasSize(2);
	}

	@Test
	void whenEntriesAreAddedToTheEnvironmentThenTheyAreIncludedInTheRequest() {
		Map<String, String> environment = new HashMap<>();
		environment.put("ALPHA", "a");
		environment.put("BRAVO", "b");
		this.buildImage.environment(environment);
		assertThat(this.buildImage.createRequest().getEnv()).containsEntry("ALPHA", "a").containsEntry("BRAVO", "b")
				.hasSize(2);
	}

	@Test
	void whenTheEnvironmentIsSetItIsIncludedInTheRequest() {
		Map<String, String> environment = new HashMap<>();
		environment.put("ALPHA", "a");
		environment.put("BRAVO", "b");
		this.buildImage.setEnvironment(environment);
		assertThat(this.buildImage.createRequest().getEnv()).containsEntry("ALPHA", "a").containsEntry("BRAVO", "b")
				.hasSize(2);
	}

	@Test
	void whenTheEnvironmentIsSetItReplacesAnyExistingEntriesAndIsIncludedInTheRequest() {
		Map<String, String> environment = new HashMap<>();
		environment.put("ALPHA", "a");
		environment.put("BRAVO", "b");
		this.buildImage.environment("C", "Charlie");
		this.buildImage.setEnvironment(environment);
		assertThat(this.buildImage.createRequest().getEnv()).containsEntry("ALPHA", "a").containsEntry("BRAVO", "b")
				.hasSize(2);
	}

	@Test
	void whenJavaVersionIsSetInEnvironmentItIsIncludedInTheRequest() {
		this.buildImage.environment("BP_JVM_VERSION", "from-env");
		this.buildImage.getTargetJavaVersion().set(JavaVersion.VERSION_1_8);
		assertThat(this.buildImage.createRequest().getEnv()).containsEntry("BP_JVM_VERSION", "from-env").hasSize(1);
	}

	@Test
	void whenTargetCompatibilityIsSetThenJavaVersionIsIncludedInTheRequest() {
		this.buildImage.getTargetJavaVersion().set(JavaVersion.VERSION_1_8);
		assertThat(this.buildImage.createRequest().getEnv()).containsEntry("BP_JVM_VERSION", "8.*").hasSize(1);
	}

	@Test
	void whenTargetCompatibilityIsSetThenJavaVersionIsAddedToEnvironment() {
		this.buildImage.environment("ALPHA", "a");
		this.buildImage.getTargetJavaVersion().set(JavaVersion.VERSION_11);
		assertThat(this.buildImage.createRequest().getEnv()).containsEntry("ALPHA", "a")
				.containsEntry("BP_JVM_VERSION", "11.*").hasSize(2);
	}

	@Test
	void whenUsingDefaultConfigurationThenRequestHasVerboseLoggingDisabled() {
		assertThat(this.buildImage.createRequest().isVerboseLogging()).isFalse();
	}

	@Test
	void whenVerboseLoggingIsEnabledThenRequestHasVerboseLoggingEnabled() {
		this.buildImage.setVerboseLogging(true);
		assertThat(this.buildImage.createRequest().isVerboseLogging()).isTrue();
	}

	@Test
	void whenUsingDefaultConfigurationThenRequestHasCleanCacheDisabled() {
		assertThat(this.buildImage.createRequest().isCleanCache()).isFalse();
	}

	@Test
	void whenCleanCacheIsEnabledThenRequestHasCleanCacheEnabled() {
		this.buildImage.setCleanCache(true);
		assertThat(this.buildImage.createRequest().isCleanCache()).isTrue();
	}

	@Test
	void whenUsingDefaultConfigurationThenRequestHasPublishDisabled() {
		assertThat(this.buildImage.createRequest().isPublish()).isFalse();
	}

	@Test
	void whenPublishIsEnabledWithoutPublishRegistryThenExceptionIsThrown() {
		this.buildImage.setPublish(true);
		assertThatExceptionOfType(GradleException.class).isThrownBy(this.buildImage::createRequest)
				.withMessageContaining("Publishing an image requires docker.publishRegistry to be configured");
	}

	@Test
	void whenNoBuilderIsConfiguredThenRequestHasDefaultBuilder() {
		assertThat(this.buildImage.createRequest().getBuilder().getName()).isEqualTo("paketobuildpacks/builder");
	}

	@Test
	void whenBuilderIsConfiguredThenRequestUsesSpecifiedBuilder() {
		this.buildImage.setBuilder("example.com/test/builder:1.2");
		assertThat(this.buildImage.createRequest().getBuilder().getName()).isEqualTo("test/builder");
	}

	@Test
	void whenNoRunImageIsConfiguredThenRequestUsesDefaultRunImage() {
		assertThat(this.buildImage.createRequest().getRunImage()).isNull();
	}

	@Test
	void whenRunImageIsConfiguredThenRequestUsesSpecifiedRunImage() {
		this.buildImage.setRunImage("example.com/test/run:1.0");
		assertThat(this.buildImage.createRequest().getRunImage().getName()).isEqualTo("test/run");
	}

	@Test
	void whenUsingDefaultConfigurationThenRequestHasAlwaysPullPolicy() {
		assertThat(this.buildImage.createRequest().getPullPolicy()).isEqualTo(PullPolicy.ALWAYS);
	}

	@Test
	void whenPullPolicyIsConfiguredThenRequestHasPullPolicy() {
		this.buildImage.setPullPolicy(PullPolicy.NEVER);
		assertThat(this.buildImage.createRequest().getPullPolicy()).isEqualTo(PullPolicy.NEVER);
	}

	@Test
	void whenNoBuildpacksAreConfiguredThenRequestUsesDefaultBuildpacks() {
		assertThat(this.buildImage.createRequest().getBuildpacks()).isEmpty();
	}

	@Test
	void whenBuildpacksAreConfiguredThenRequestHasBuildpacks() {
		this.buildImage.setBuildpacks(Arrays.asList("example/buildpack1", "example/buildpack2"));
		assertThat(this.buildImage.createRequest().getBuildpacks()).containsExactly(
				BuildpackReference.of("example/buildpack1"), BuildpackReference.of("example/buildpack2"));
	}

	@Test
	void whenEntriesAreAddedToBuildpacksThenRequestHasBuildpacks() {
		this.buildImage.buildpacks(Arrays.asList("example/buildpack1", "example/buildpack2"));
		assertThat(this.buildImage.createRequest().getBuildpacks()).containsExactly(
				BuildpackReference.of("example/buildpack1"), BuildpackReference.of("example/buildpack2"));
	}

	@Test
	void whenIndividualEntriesAreAddedToBuildpacksThenRequestHasBuildpacks() {
		this.buildImage.buildpack("example/buildpack1");
		this.buildImage.buildpack("example/buildpack2");
		assertThat(this.buildImage.createRequest().getBuildpacks()).containsExactly(
				BuildpackReference.of("example/buildpack1"), BuildpackReference.of("example/buildpack2"));
	}

	@Test
	void whenNoBindingsAreConfiguredThenRequestHasNoBindings() {
		assertThat(this.buildImage.createRequest().getBindings()).isEmpty();
	}

	@Test
	void whenBindingsAreConfiguredThenRequestHasBindings() {
		this.buildImage.setBindings(Arrays.asList("host-src:container-dest:ro", "volume-name:container-dest:rw"));
		assertThat(this.buildImage.createRequest().getBindings())
				.containsExactly(Binding.of("host-src:container-dest:ro"), Binding.of("volume-name:container-dest:rw"));
	}

	@Test
	void whenEntriesAreAddedToBindingsThenRequestHasBindings() {
		this.buildImage.bindings(Arrays.asList("host-src:container-dest:ro", "volume-name:container-dest:rw"));
		assertThat(this.buildImage.createRequest().getBindings())
				.containsExactly(Binding.of("host-src:container-dest:ro"), Binding.of("volume-name:container-dest:rw"));
	}

	@Test
	void whenIndividualEntriesAreAddedToBindingsThenRequestHasBindings() {
		this.buildImage.binding("host-src:container-dest:ro");
		this.buildImage.binding("volume-name:container-dest:rw");
		assertThat(this.buildImage.createRequest().getBindings())
				.containsExactly(Binding.of("host-src:container-dest:ro"), Binding.of("volume-name:container-dest:rw"));
	}

	@Test
	void whenNetworkIsConfiguredThenRequestHasNetwork() {
		this.buildImage.setNetwork("test");
		assertThat(this.buildImage.createRequest().getNetwork()).isEqualTo("test");
	}

	@Test
	void whenNoTagsAreConfiguredThenRequestHasNoTags() {
		assertThat(this.buildImage.createRequest().getTags()).isEmpty();
	}

	@Test
	void whenTagsAreConfiguredThenRequestHasTags() {
		this.buildImage.setTags(
				Arrays.asList("my-app:latest", "example.com/my-app:0.0.1-SNAPSHOT", "example.com/my-app:latest"));
		assertThat(this.buildImage.createRequest().getTags()).containsExactly(ImageReference.of("my-app:latest"),
				ImageReference.of("example.com/my-app:0.0.1-SNAPSHOT"), ImageReference.of("example.com/my-app:latest"));
	}

	@Test
	void whenEntriesAreAddedToTagsThenRequestHasTags() {
		this.buildImage
				.tags(Arrays.asList("my-app:latest", "example.com/my-app:0.0.1-SNAPSHOT", "example.com/my-app:latest"));
		assertThat(this.buildImage.createRequest().getTags()).containsExactly(ImageReference.of("my-app:latest"),
				ImageReference.of("example.com/my-app:0.0.1-SNAPSHOT"), ImageReference.of("example.com/my-app:latest"));
	}

	@Test
	void whenIndividualEntriesAreAddedToTagsThenRequestHasTags() {
		this.buildImage.tag("my-app:latest");
		this.buildImage.tag("example.com/my-app:0.0.1-SNAPSHOT");
		this.buildImage.tag("example.com/my-app:latest");
		assertThat(this.buildImage.createRequest().getTags()).containsExactly(ImageReference.of("my-app:latest"),
				ImageReference.of("example.com/my-app:0.0.1-SNAPSHOT"), ImageReference.of("example.com/my-app:latest"));
	}

}
