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

package org.springframework.boot.docker.compose.service.connection.activemq;

import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConnectionDetails;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisMode;
import org.springframework.boot.docker.compose.service.connection.test.DockerComposeTest;
import org.springframework.boot.testsupport.container.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ArtemisDockerComposeConnectionDetailsFactory}.
 *
 * @author Eddú Meléndez
 */
class ArtemisDockerComposeConnectionDetailsFactoryIntegrationTests {

	@DockerComposeTest(composeFile = "artemis-compose.yaml", image = TestImage.ARTEMIS)
	void runCreatesConnectionDetails(ArtemisConnectionDetails connectionDetails) {
		assertThat(connectionDetails.getMode()).isEqualTo(ArtemisMode.NATIVE);
		assertThat(connectionDetails.getBrokerUrl()).isNotNull().startsWith("tcp://");
		assertThat(connectionDetails.getUser()).isEqualTo("root");
		assertThat(connectionDetails.getPassword()).isEqualTo("secret");
	}

}
