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

package org.springframework.boot.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.example.CustomQualifier;
import org.springframework.boot.test.mock.mockito.example.CustomQualifierExampleService;
import org.springframework.boot.test.mock.mockito.example.ExampleService;
import org.springframework.boot.test.mock.mockito.example.ExampleServiceCaller;
import org.springframework.boot.test.mock.mockito.example.RealExampleService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Test {@link MockBean @MockBean} on a test class field can be used to replace existing
 * bean while preserving qualifiers.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @deprecated since 3.4.0 for removal in 4.0.0
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.4.0", forRemoval = true)
@ExtendWith(SpringExtension.class)
class MockBeanOnTestFieldForExistingBeanWithQualifierIntegrationTests {

	@MockBean
	@CustomQualifier
	private ExampleService service;

	@Autowired
	private ExampleServiceCaller caller;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void testMocking() {
		this.caller.sayGreeting();
		then(this.service).should().greeting();
	}

	@Test
	void onlyQualifiedBeanIsReplaced() {
		assertThat(this.applicationContext.getBean("service")).isSameAs(this.service);
		ExampleService anotherService = this.applicationContext.getBean("anotherService", ExampleService.class);
		assertThat(anotherService.greeting()).isEqualTo("Another");
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfig {

		@Bean
		CustomQualifierExampleService service() {
			return new CustomQualifierExampleService();
		}

		@Bean
		ExampleService anotherService() {
			return new RealExampleService("Another");
		}

		@Bean
		ExampleServiceCaller controller(@CustomQualifier ExampleService service) {
			return new ExampleServiceCaller(service);
		}

	}

}
