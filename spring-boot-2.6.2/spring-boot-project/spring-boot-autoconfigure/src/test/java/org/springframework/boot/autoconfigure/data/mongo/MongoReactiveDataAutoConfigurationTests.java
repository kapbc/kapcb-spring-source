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

package org.springframework.boot.autoconfigure.data.mongo;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoReactiveDataAutoConfiguration}.
 *
 * @author Mark Paluch
 * @author Artsiom Yudovin
 */
class MongoReactiveDataAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class,
					MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class));

	@Test
	void templateExists() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ReactiveMongoTemplate.class));
	}

	@Test
	void whenNoGridFsDatabaseIsConfiguredTheGridFsTemplateUsesTheMainDatabase() {
		this.contextRunner.run((context) -> assertThat(grisFsTemplateDatabaseName(context)).isEqualTo("test"));
	}

	@Test
	void whenGridFsDatabaseIsConfiguredThenGridFsTemplateUsesIt() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.gridfs.database:grid")
				.run((context) -> assertThat(grisFsTemplateDatabaseName(context)).isEqualTo("grid"));
	}

	@Test
	void whenGridFsBucketIsConfiguredThenGridFsTemplateUsesIt() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.gridfs.bucket:test-bucket").run((context) -> {
			assertThat(context).hasSingleBean(ReactiveGridFsTemplate.class);
			ReactiveGridFsTemplate template = context.getBean(ReactiveGridFsTemplate.class);
			assertThat(template).hasFieldOrPropertyWithValue("bucket", "test-bucket");
		});
	}

	@Test
	void backsOffIfMongoClientBeanIsNotPresent() {
		ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
				.of(PropertyPlaceholderAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class));
		runner.run((context) -> assertThat(context).doesNotHaveBean(MongoReactiveDataAutoConfiguration.class));
	}

	private String grisFsTemplateDatabaseName(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(ReactiveGridFsTemplate.class);
		ReactiveGridFsTemplate template = context.getBean(ReactiveGridFsTemplate.class);
		ReactiveMongoDatabaseFactory factory = (ReactiveMongoDatabaseFactory) ReflectionTestUtils.getField(template,
				"dbFactory");
		return factory.getMongoDatabase().block().getName();
	}

}
