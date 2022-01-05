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

package org.springframework.boot.autoconfigure.batch;

import java.util.Collection;
import java.util.Collections;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BatchAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Vedran Pavic
 * @author Kazuki Shimizu
 */
class BatchAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(BatchAutoConfiguration.class, TransactionAutoConfiguration.class));

	@Test
	void testDefaultContext() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(JobLauncher.class);
					assertThat(context).hasSingleBean(JobExplorer.class);
					assertThat(context.getBean(BatchProperties.class).getJdbc().getInitializeSchema())
							.isEqualTo(DatabaseInitializationMode.EMBEDDED);
					assertThat(new JdbcTemplate(context.getBean(DataSource.class))
							.queryForList("select * from BATCH_JOB_EXECUTION")).isEmpty();
				});
	}

	@Test
	void whenThereIsNoDataSourceAutoConfigurationBacksOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(BatchConfigurer.class));
	}

	@Test
	void whenThereIsAnEntityManagerFactoryButNoDataSourceAutoConfigurationBacksOff() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, EntityManagerFactoryConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(BatchConfigurer.class));
	}

	@Test
	void testNoBatchConfiguration() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					assertThat(context).doesNotHaveBean(JobLauncher.class);
					assertThat(context).doesNotHaveBean(JobRepository.class);
				});
	}

	@Test
	void testDefinesAndLaunchesJob() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(JobLauncher.class);
					context.getBean(JobLauncherApplicationRunner.class)
							.run(new DefaultApplicationArguments("jobParam=test"));
					JobParameters jobParameters = new JobParametersBuilder().addString("jobParam", "test")
							.toJobParameters();
					assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", jobParameters))
							.isNotNull();
				});
	}

	@Test
	void testDefinesAndLaunchesJobIgnoreOptionArguments() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(JobLauncher.class);
					context.getBean(JobLauncherApplicationRunner.class)
							.run(new DefaultApplicationArguments("--spring.property=value", "jobParam=test"));
					JobParameters jobParameters = new JobParametersBuilder().addString("jobParam", "test")
							.toJobParameters();
					assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", jobParameters))
							.isNotNull();
				});
	}

	@Test
	void testDefinesAndLaunchesNamedJob() {
		this.contextRunner
				.withUserConfiguration(NamedJobConfigurationWithRegisteredJob.class,
						EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.batch.job.names:discreteRegisteredJob").run((context) -> {
					assertThat(context).hasSingleBean(JobLauncher.class);
					context.getBean(JobLauncherApplicationRunner.class).run();
					assertThat(context.getBean(JobRepository.class).getLastJobExecution("discreteRegisteredJob",
							new JobParameters())).isNotNull();
				});
	}

	@Test
	void testDefinesAndLaunchesLocalJob() {
		this.contextRunner
				.withUserConfiguration(NamedJobConfigurationWithLocalJob.class, EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.batch.job.names:discreteLocalJob").run((context) -> {
					assertThat(context).hasSingleBean(JobLauncher.class);
					context.getBean(JobLauncherApplicationRunner.class).run();
					assertThat(context.getBean(JobRepository.class).getLastJobExecution("discreteLocalJob",
							new JobParameters())).isNotNull();
				});
	}

	@Test
	void testDisableLaunchesJob() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.batch.job.enabled:false").run((context) -> {
					assertThat(context).hasSingleBean(JobLauncher.class);
					assertThat(context).doesNotHaveBean(CommandLineRunner.class);
				});
	}

	@Test
	void testDisableSchemaLoader() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.batch.jdbc.initialize-schema:never")
				.run(assertDatasourceIsNotInitialized());
	}

	@Test
	@Deprecated
	void testDisableSchemaLoaderWithDeprecatedProperty() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.batch.initialize-schema:never")
				.run(assertDatasourceIsNotInitialized());
	}

	private ContextConsumer<AssertableApplicationContext> assertDatasourceIsNotInitialized() {
		return (context) -> {
			assertThat(context).hasSingleBean(JobLauncher.class);
			assertThat(context.getBean(BatchProperties.class).getJdbc().getInitializeSchema())
					.isEqualTo(DatabaseInitializationMode.NEVER);
			assertThat(context).doesNotHaveBean(BatchDataSourceScriptDatabaseInitializer.class);
			assertThatExceptionOfType(BadSqlGrammarException.class)
					.isThrownBy(() -> new JdbcTemplate(context.getBean(DataSource.class))
							.queryForList("select * from BATCH_JOB_EXECUTION"));
		};
	}

	@Test
	void testUsingJpa() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class,
				HibernateJpaAutoConfiguration.class).run((context) -> {
					PlatformTransactionManager transactionManager = context.getBean(PlatformTransactionManager.class);
					// It's a lazy proxy, but it does render its target if you ask for
					// toString():
					assertThat(transactionManager.toString().contains("JpaTransactionManager")).isTrue();
					assertThat(context).hasSingleBean(EntityManagerFactory.class);
					// Ensure the JobRepository can be used (no problem with isolation
					// level)
					assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", new JobParameters()))
							.isNull();
				});
	}

	@Test
	void testRenamePrefix() {
		this.contextRunner
				.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class,
						HibernateJpaAutoConfiguration.class)
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.batch.jdbc.schema:classpath:batch/custom-schema-hsql.sql",
						"spring.batch.jdbc.tablePrefix:PREFIX_")
				.run(assertCustomTablePrefix());
	}

	@Test
	@Deprecated
	void testRenamePrefixWithDeprecatedProperty() {
		this.contextRunner
				.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class,
						HibernateJpaAutoConfiguration.class)
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.batch.schema:classpath:batch/custom-schema-hsql.sql",
						"spring.batch.tablePrefix:PREFIX_")
				.run(assertCustomTablePrefix());
	}

	private ContextConsumer<AssertableApplicationContext> assertCustomTablePrefix() {
		return (context) -> {
			assertThat(context).hasSingleBean(JobLauncher.class);
			assertThat(context.getBean(BatchProperties.class).getJdbc().getInitializeSchema())
					.isEqualTo(DatabaseInitializationMode.EMBEDDED);
			assertThat(new JdbcTemplate(context.getBean(DataSource.class))
					.queryForList("select * from PREFIX_JOB_EXECUTION")).isEmpty();
			JobExplorer jobExplorer = context.getBean(JobExplorer.class);
			assertThat(jobExplorer.findRunningJobExecutions("test")).isEmpty();
			JobRepository jobRepository = context.getBean(JobRepository.class);
			assertThat(jobRepository.getLastJobExecution("test", new JobParameters())).isNull();
		};
	}

	@Test
	void testCustomizeJpaTransactionManagerUsingProperties() {
		this.contextRunner
				.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class,
						HibernateJpaAutoConfiguration.class)
				.withPropertyValues("spring.transaction.default-timeout:30",
						"spring.transaction.rollback-on-commit-failure:true")
				.run((context) -> {
					assertThat(context).hasSingleBean(BatchConfigurer.class);
					JpaTransactionManager transactionManager = JpaTransactionManager.class
							.cast(context.getBean(BatchConfigurer.class).getTransactionManager());
					assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
					assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
				});
	}

	@Test
	void testCustomizeDataSourceTransactionManagerUsingProperties() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.transaction.default-timeout:30",
						"spring.transaction.rollback-on-commit-failure:true")
				.run((context) -> {
					assertThat(context).hasSingleBean(BatchConfigurer.class);
					DataSourceTransactionManager transactionManager = DataSourceTransactionManager.class
							.cast(context.getBean(BatchConfigurer.class).getTransactionManager());
					assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
					assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
				});
	}

	@Test
	void testBatchDataSource() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, BatchDataSourceConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(BatchConfigurer.class)
							.hasSingleBean(BatchDataSourceScriptDatabaseInitializer.class).hasBean("batchDataSource");
					DataSource batchDataSource = context.getBean("batchDataSource", DataSource.class);
					assertThat(context.getBean(BatchConfigurer.class)).hasFieldOrPropertyWithValue("dataSource",
							batchDataSource);
					assertThat(context.getBean(BatchDataSourceScriptDatabaseInitializer.class))
							.hasFieldOrPropertyWithValue("dataSource", batchDataSource);
				});
	}

	@Test
	void jobRepositoryBeansDependOnBatchDataSourceInitializer() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
					String[] jobRepositoryNames = beanFactory.getBeanNamesForType(JobRepository.class);
					assertThat(jobRepositoryNames).isNotEmpty();
					for (String jobRepositoryName : jobRepositoryNames) {
						assertThat(beanFactory.getBeanDefinition(jobRepositoryName).getDependsOn())
								.contains("batchDataSourceInitializer");
					}
				});
	}

	@Test
	void jobRepositoryBeansDependOnFlyway() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withUserConfiguration(FlywayAutoConfiguration.class)
				.withPropertyValues("spring.batch.initialize-schema=never").run((context) -> {
					ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
					String[] jobRepositoryNames = beanFactory.getBeanNamesForType(JobRepository.class);
					assertThat(jobRepositoryNames).isNotEmpty();
					for (String jobRepositoryName : jobRepositoryNames) {
						assertThat(beanFactory.getBeanDefinition(jobRepositoryName).getDependsOn()).contains("flyway",
								"flywayInitializer");
					}
				});
	}

	@Test
	void jobRepositoryBeansDependOnLiquibase() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.withUserConfiguration(LiquibaseAutoConfiguration.class)
				.withPropertyValues("spring.batch.initialize-schema=never").run((context) -> {
					ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
					String[] jobRepositoryNames = beanFactory.getBeanNamesForType(JobRepository.class);
					assertThat(jobRepositoryNames).isNotEmpty();
					for (String jobRepositoryName : jobRepositoryNames) {
						assertThat(beanFactory.getBeanDefinition(jobRepositoryName).getDependsOn())
								.contains("liquibase");
					}
				});
	}

	@Test
	void whenTheUserDefinesTheirOwnBatchDatabaseInitializerThenTheAutoConfiguredInitializerBacksOff() {
		this.contextRunner
				.withUserConfiguration(TestConfiguration.class, CustomBatchDatabaseInitializerConfiguration.class)
				.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(BatchDataSourceScriptDatabaseInitializer.class)
						.doesNotHaveBean("batchDataSourceScriptDatabaseInitializer").hasBean("customInitializer"));
	}

	@Test
	@Deprecated
	@SuppressWarnings("deprecation")
	void whenTheUserDefinesTheirOwnBatchDataSourceInitializerThenTheAutoConfiguredInitializerBacksOff() {
		this.contextRunner
				.withUserConfiguration(TestConfiguration.class, CustomBatchDataSourceInitializerConfiguration.class)
				.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(BatchDataSourceScriptDatabaseInitializer.class)
						.hasSingleBean(BatchDataSourceInitializer.class).hasBean("customInitializer"));
	}

	@Test
	void whenTheUserDefinesTheirOwnDatabaseInitializerThenTheAutoConfiguredBatchInitializerRemains() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class, CustomDatabaseInitializerConfiguration.class)
				.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.run((context) -> assertThat(context).hasSingleBean(BatchDataSourceScriptDatabaseInitializer.class)
						.hasBean("customInitializer"));
	}

	@Configuration(proxyBeanMethods = false)
	protected static class BatchDataSourceConfiguration {

		@Bean
		@Primary
		public DataSource normalDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:normal").username("sa").build();
		}

		@BatchDataSource
		@Bean
		public DataSource batchDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:batchdatasource").username("sa").build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {

	}

	@EnableBatchProcessing
	@TestAutoConfigurationPackage(City.class)
	static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class EntityManagerFactoryConfiguration {

		@Bean
		EntityManagerFactory entityManagerFactory() {
			return mock(EntityManagerFactory.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableBatchProcessing
	static class NamedJobConfigurationWithRegisteredJob {

		@Autowired
		private JobRegistry jobRegistry;

		@Autowired
		private JobRepository jobRepository;

		@Bean
		JobRegistryBeanPostProcessor registryProcessor() {
			JobRegistryBeanPostProcessor processor = new JobRegistryBeanPostProcessor();
			processor.setJobRegistry(this.jobRegistry);
			return processor;
		}

		@Bean
		Job discreteJob() {
			AbstractJob job = new AbstractJob("discreteRegisteredJob") {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableBatchProcessing
	static class NamedJobConfigurationWithLocalJob {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		Job discreteJob() {
			AbstractJob job = new AbstractJob("discreteLocalJob") {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableBatchProcessing
	static class JobConfiguration {

		@Autowired
		private JobRepository jobRepository;

		@Bean
		Job job() {
			AbstractJob job = new AbstractJob() {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(this.jobRepository);
			return job;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomBatchDatabaseInitializerConfiguration {

		@Bean
		BatchDataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource, BatchProperties properties) {
			return new BatchDataSourceScriptDatabaseInitializer(dataSource, properties.getJdbc());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDatabaseInitializerConfiguration {

		@Bean
		DataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource) {
			return new DataSourceScriptDatabaseInitializer(dataSource, new DatabaseInitializationSettings());
		}

	}

	@Deprecated
	@Configuration(proxyBeanMethods = false)
	static class CustomBatchDataSourceInitializerConfiguration {

		@Bean
		BatchDataSourceInitializer customInitializer(DataSource dataSource, ResourceLoader resourceLoader,
				BatchProperties properties) {
			return new BatchDataSourceInitializer(dataSource, resourceLoader, properties);
		}

	}

}
