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

package org.springframework.boot.gradle.tasks.buildinfo;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.initialization.GradlePropertiesController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.gradle.junit.GradleProjectBuilder;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BuildInfo}.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 */
@ClassPathExclusions("kotlin-daemon-client-*")
class BuildInfoTests {

	@TempDir
	File temp;

	@Test
	void basicExecution() {
		Properties properties = buildInfoProperties(createTask(createProject("test")));
		assertThat(properties).containsKey("build.time");
		assertThat(properties).doesNotContainKey("build.artifact");
		assertThat(properties).doesNotContainKey("build.group");
		assertThat(properties).containsEntry("build.name", "test");
		assertThat(properties).containsEntry("build.version", "unspecified");
	}

	@Test
	void customArtifactIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setArtifact("custom");
		assertThat(buildInfoProperties(task)).containsEntry("build.artifact", "custom");
	}

	@Test
	void artifactCanBeRemovedFromPropertiesUsingNull() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setArtifact(null);
		assertThat(buildInfoProperties(task)).doesNotContainKey("build.artifact");
	}

	@Test
	void artifactCanBeRemovedFromPropertiesUsingEmptyString() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setArtifact("");
		assertThat(buildInfoProperties(task)).doesNotContainKey("build.artifact");
	}

	@Test
	void projectGroupIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProject().setGroup("com.example");
		assertThat(buildInfoProperties(task)).containsEntry("build.group", "com.example");
	}

	@Test
	void customGroupIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setGroup("com.example");
		assertThat(buildInfoProperties(task)).containsEntry("build.group", "com.example");
	}

	@Test
	void groupCanBeRemovedFromPropertiesUsingNull() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setGroup(null);
		assertThat(buildInfoProperties(task)).doesNotContainKey("build.group");
	}

	@Test
	void groupCanBeRemovedFromPropertiesUsingEmptyString() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setGroup("");
		assertThat(buildInfoProperties(task)).doesNotContainKey("build.group");
	}

	@Test
	void customNameIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setName("Example");
		assertThat(buildInfoProperties(task)).containsEntry("build.name", "Example");
	}

	@Test
	void nameCanBeRemovedFromPropertiesUsingNull() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setName(null);
		assertThat(buildInfoProperties(task)).doesNotContainKey("build.name");
	}

	@Test
	void nameCanBeRemovedFromPropertiesUsingEmptyString() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setName("");
		assertThat(buildInfoProperties(task)).doesNotContainKey("build.name");
	}

	@Test
	void projectVersionIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProject().setVersion("1.2.3");
		assertThat(buildInfoProperties(task)).containsEntry("build.version", "1.2.3");
	}

	@Test
	void customVersionIsReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setVersion("2.3.4");
		assertThat(buildInfoProperties(task)).containsEntry("build.version", "2.3.4");
	}

	@Test
	void versionCanBeRemovedFromPropertiesUsingNull() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setVersion(null);
		assertThat(buildInfoProperties(task)).doesNotContainKey("build.version");
	}

	@Test
	void versionCanBeRemovedFromPropertiesUsingEmptyString() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setVersion("");
		assertThat(buildInfoProperties(task)).doesNotContainKey("build.version");
	}

	@Test
	void timeIsSetInProperties() {
		BuildInfo task = createTask(createProject("test"));
		assertThat(buildInfoProperties(task)).containsEntry("build.time",
				DateTimeFormatter.ISO_INSTANT.format(task.getProperties().getTime()));
	}

	@Test
	void timeCanBeRemovedFromProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setTime(null);
		assertThat(buildInfoProperties(task)).doesNotContainKey("build.time");
	}

	@Test
	void timeCanBeCustomizedInProperties() {
		Instant now = Instant.now();
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().setTime(now);
		assertThat(buildInfoProperties(task)).containsEntry("build.time",
				DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(now.toEpochMilli())));
	}

	@Test
	void additionalPropertiesAreReflectedInProperties() {
		BuildInfo task = createTask(createProject("test"));
		task.getProperties().getAdditional().put("a", "alpha");
		task.getProperties().getAdditional().put("b", "bravo");
		assertThat(buildInfoProperties(task)).containsEntry("build.a", "alpha");
		assertThat(buildInfoProperties(task)).containsEntry("build.b", "bravo");
	}

	private Project createProject(String projectName) {
		File projectDir = new File(this.temp, projectName);
		Project project = GradleProjectBuilder.builder().withProjectDir(projectDir).withName(projectName).build();
		((ProjectInternal) project).getServices().get(GradlePropertiesController.class)
				.loadGradlePropertiesFrom(projectDir);
		return project;
	}

	private BuildInfo createTask(Project project) {
		return project.getTasks().create("testBuildInfo", BuildInfo.class);
	}

	private Properties buildInfoProperties(BuildInfo task) {
		task.generateBuildProperties();
		return buildInfoProperties(new File(task.getDestinationDir(), "build-info.properties"));
	}

	private Properties buildInfoProperties(File file) {
		assertThat(file).isFile();
		Properties properties = new Properties();
		try (FileReader reader = new FileReader(file)) {
			properties.load(reader);
			return properties;
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
