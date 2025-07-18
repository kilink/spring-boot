/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.autoconfigure.jdbc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdbcTemplateAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Dan Zheng
 */
class JdbcTemplateAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.generate-unique-name=true")
		.withConfiguration(
				AutoConfigurations.of(DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class));

	@Test
	void testJdbcTemplateExists() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(JdbcOperations.class);
			JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
			assertThat(jdbcTemplate.getDataSource()).isEqualTo(context.getBean(DataSource.class));
			assertThat(jdbcTemplate.isIgnoreWarnings()).isEqualTo(true);
			assertThat(jdbcTemplate.getFetchSize()).isEqualTo(-1);
			assertThat(jdbcTemplate.getQueryTimeout()).isEqualTo(-1);
			assertThat(jdbcTemplate.getMaxRows()).isEqualTo(-1);
			assertThat(jdbcTemplate.isSkipResultsProcessing()).isEqualTo(false);
			assertThat(jdbcTemplate.isSkipUndeclaredResults()).isEqualTo(false);
			assertThat(jdbcTemplate.isResultsMapCaseInsensitive()).isEqualTo(false);
		});
	}

	@Test
	void testJdbcTemplateWithCustomProperties() {
		this.contextRunner
			.withPropertyValues("spring.jdbc.template.ignore-warnings:false", "spring.jdbc.template.fetch-size:100",
					"spring.jdbc.template.query-timeout:60", "spring.jdbc.template.max-rows:1000",
					"spring.jdbc.template.skip-results-processing:true",
					"spring.jdbc.template.skip-undeclared-results:true",
					"spring.jdbc.template.results-map-case-insensitive:true")
			.run((context) -> {
				assertThat(context).hasSingleBean(JdbcOperations.class);
				JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
				assertThat(jdbcTemplate.getDataSource()).isNotNull();
				assertThat(jdbcTemplate.isIgnoreWarnings()).isEqualTo(false);
				assertThat(jdbcTemplate.getFetchSize()).isEqualTo(100);
				assertThat(jdbcTemplate.getQueryTimeout()).isEqualTo(60);
				assertThat(jdbcTemplate.getMaxRows()).isEqualTo(1000);
				assertThat(jdbcTemplate.isSkipResultsProcessing()).isEqualTo(true);
				assertThat(jdbcTemplate.isSkipUndeclaredResults()).isEqualTo(true);
				assertThat(jdbcTemplate.isResultsMapCaseInsensitive()).isEqualTo(true);
			});
	}

	@Test
	void testJdbcTemplateExistsWithCustomDataSource() {
		this.contextRunner.withUserConfiguration(TestDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JdbcOperations.class);
			JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
			assertThat(jdbcTemplate.getDataSource()).isEqualTo(context.getBean("customDataSource"));
		});
	}

	@Test
	void testNamedParameterJdbcTemplateExists() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
			NamedParameterJdbcTemplate namedParameterJdbcTemplate = context.getBean(NamedParameterJdbcTemplate.class);
			assertThat(namedParameterJdbcTemplate.getJdbcOperations()).isEqualTo(context.getBean(JdbcOperations.class));
		});
	}

	@Test
	void testMultiDataSource() {
		this.contextRunner.withUserConfiguration(MultiDataSourceConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(JdbcOperations.class);
			assertThat(context).doesNotHaveBean(NamedParameterJdbcOperations.class);
		});
	}

	@Test
	void testMultiJdbcTemplate() {
		this.contextRunner.withUserConfiguration(MultiJdbcTemplateConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(NamedParameterJdbcOperations.class));
	}

	@Test
	void testMultiDataSourceUsingPrimary() {
		this.contextRunner.withUserConfiguration(MultiDataSourceUsingPrimaryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JdbcOperations.class);
			assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
			assertThat(context.getBean(JdbcTemplate.class).getDataSource())
				.isEqualTo(context.getBean("test1DataSource"));
		});
	}

	@Test
	void testMultiJdbcTemplateUsingPrimary() {
		this.contextRunner.withUserConfiguration(MultiJdbcTemplateUsingPrimaryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
			assertThat(context.getBean(NamedParameterJdbcTemplate.class).getJdbcOperations())
				.isEqualTo(context.getBean("test1Template"));
		});
	}

	@Test
	void testExistingCustomJdbcTemplate() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JdbcOperations.class);
			assertThat(context.getBean(JdbcOperations.class)).isEqualTo(context.getBean("customJdbcOperations"));
		});
	}

	@Test
	void testExistingCustomNamedParameterJdbcTemplate() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
			assertThat(context.getBean(NamedParameterJdbcOperations.class))
				.isEqualTo(context.getBean("customNamedParameterJdbcOperations"));
		});
	}

	@Test
	@WithResource(name = "schema.sql", content = """
			CREATE TABLE BAR (
			  id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
			  name VARCHAR(30)
			);
			""")
	@WithResource(name = "data.sql", content = "INSERT INTO BAR VALUES (1, 'Andy');")
	void testDependencyToScriptBasedDataSourceInitialization() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(SqlInitializationAutoConfiguration.class))
			.withUserConfiguration(DataSourceInitializationValidator.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context.getBean(DataSourceInitializationValidator.class).count).isOne();
			});
	}

	@Test
	@WithResource(name = "db/city/V1__init.sql", content = """
			CREATE SEQUENCE city_seq INCREMENT BY 50;
			CREATE TABLE CITY (
			  id         BIGINT GENERATED BY DEFAULT AS IDENTITY,
			  name VARCHAR(30),
			  state VARCHAR(30),
			  country VARCHAR(30),
			  map VARCHAR(30)
			);
			""")
	void testDependencyToFlyway() {
		this.contextRunner.withUserConfiguration(DataSourceMigrationValidator.class)
			.withPropertyValues("spring.flyway.locations:classpath:db/city")
			.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context.getBean(DataSourceMigrationValidator.class).count).isZero();
			});
	}

	@Test
	@WithResource(name = "db/city/V1__init.sql", content = """
			CREATE SEQUENCE city_seq INCREMENT BY 50;
			CREATE TABLE CITY (
			  id         BIGINT GENERATED BY DEFAULT AS IDENTITY,
			  name VARCHAR(30),
			  state VARCHAR(30),
			  country VARCHAR(30),
			  map VARCHAR(30)
			);
			""")
	void testDependencyToFlywayWithJdbcTemplateMixed() {
		this.contextRunner.withUserConfiguration(NamedParameterDataSourceMigrationValidator.class)
			.withPropertyValues("spring.flyway.locations:classpath:db/city")
			.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context.getBean(JdbcTemplate.class)).isNotNull();
				assertThat(context.getBean(NamedParameterDataSourceMigrationValidator.class).count).isZero();
			});
	}

	@Test
	@WithDbChangelogCityYamlResource
	void testDependencyToLiquibase() {
		this.contextRunner.withUserConfiguration(DataSourceMigrationValidator.class)
			.withPropertyValues("spring.liquibase.change-log:classpath:db/changelog/db.changelog-city.yaml")
			.withConfiguration(AutoConfigurations.of(LiquibaseAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context.getBean(DataSourceMigrationValidator.class).count).isZero();
			});
	}

	@Test
	@WithDbChangelogCityYamlResource
	void testDependencyToLiquibaseWithJdbcTemplateMixed() {
		this.contextRunner.withUserConfiguration(NamedParameterDataSourceMigrationValidator.class)
			.withPropertyValues("spring.liquibase.change-log:classpath:db/changelog/db.changelog-city.yaml")
			.withConfiguration(AutoConfigurations.of(LiquibaseAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context.getBean(JdbcTemplate.class)).isNotNull();
				assertThat(context.getBean(NamedParameterDataSourceMigrationValidator.class).count).isZero();
			});
	}

	@Test
	void shouldConfigureJdbcTemplateWithSQLExceptionTranslatorIfPresent() {
		SQLStateSQLExceptionTranslator sqlExceptionTranslator = new SQLStateSQLExceptionTranslator();
		this.contextRunner.withBean(SQLExceptionTranslator.class, () -> sqlExceptionTranslator).run((context) -> {
			assertThat(context).hasSingleBean(JdbcTemplate.class);
			JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
			assertThat(jdbcTemplate.getExceptionTranslator()).isSameAs(sqlExceptionTranslator);
		});
	}

	@Test
	void shouldNotConfigureJdbcTemplateWithSQLExceptionTranslatorIfNotUnique() {
		SQLStateSQLExceptionTranslator sqlExceptionTranslator1 = new SQLStateSQLExceptionTranslator();
		SQLStateSQLExceptionTranslator sqlExceptionTranslator2 = new SQLStateSQLExceptionTranslator();
		this.contextRunner
			.withBean("sqlExceptionTranslator1", SQLExceptionTranslator.class, () -> sqlExceptionTranslator1)
			.withBean("sqlExceptionTranslator2", SQLExceptionTranslator.class, () -> sqlExceptionTranslator2)
			.run((context) -> {
				assertThat(context).hasSingleBean(JdbcTemplate.class);
				JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
				assertThat(jdbcTemplate.getExceptionTranslator()).isNotSameAs(sqlExceptionTranslator1)
					.isNotSameAs(sqlExceptionTranslator2);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConfiguration {

		@Bean
		JdbcOperations customJdbcOperations(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		NamedParameterJdbcOperations customNamedParameterJdbcOperations(DataSource dataSource) {
			return new NamedParameterJdbcTemplate(dataSource);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestDataSourceConfiguration {

		@Bean
		DataSource customDataSource() {
			return new TestDataSource();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultiJdbcTemplateConfiguration {

		@Bean
		JdbcTemplate test1Template() {
			return mock(JdbcTemplate.class);
		}

		@Bean
		JdbcTemplate test2Template() {
			return mock(JdbcTemplate.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultiJdbcTemplateUsingPrimaryConfiguration {

		@Bean
		@Primary
		JdbcTemplate test1Template() {
			return mock(JdbcTemplate.class);
		}

		@Bean
		JdbcTemplate test2Template() {
			return mock(JdbcTemplate.class);
		}

	}

	static class DataSourceInitializationValidator {

		private final Integer count;

		DataSourceInitializationValidator(JdbcTemplate jdbcTemplate) {
			this.count = jdbcTemplate.queryForObject("SELECT COUNT(*) from BAR", Integer.class);
		}

	}

	static class DataSourceMigrationValidator {

		private final Integer count;

		DataSourceMigrationValidator(JdbcTemplate jdbcTemplate) {
			this.count = jdbcTemplate.queryForObject("SELECT COUNT(*) from CITY", Integer.class);
		}

	}

	static class NamedParameterDataSourceMigrationValidator {

		private final Integer count;

		NamedParameterDataSourceMigrationValidator(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
			this.count = namedParameterJdbcTemplate.queryForObject("SELECT COUNT(*) from CITY", Collections.emptyMap(),
					Integer.class);
		}

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "db/changelog/db.changelog-city.yaml", content = """
			databaseChangeLog:
			- changeSet:
			    id: 1
			    author: dsyer
			    changes:
			      - createSequence:
			          sequenceName: city_seq
			          incrementBy: 50
			      - createTable:
			          tableName: city
			          columns:
			            - column:
			                name: id
			                type: bigint
			                autoIncrement: true
			                constraints:
			                  primaryKey: true
			                  nullable: false
			            - column:
			                name: name
			                type: varchar(50)
			                constraints:
			                  nullable: false
			""")
	@interface WithDbChangelogCityYamlResource {

	}

}
