/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.spinnaker;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.CloudFoundryOperationsBuilder;
import org.cloudfoundry.spring.client.SpringCloudFoundryClient;

import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerProperties;

/**
 * @author Greg Turnquist
 */
public class DefaultAppDeployerFactory implements CloudFoundryAppDeployerFactory {

	private final Map<String, CloudFoundryAppDeployer> cachedDeployers = new HashMap<>();

	public DefaultAppDeployerFactory() {
	}

	public CloudFoundryAppDeployer getObject(String api, String org, String space, String email, String password, String namespace) {
		return getObject(new CloudFoundryDeployerProperties(), api, org, space, email, password, namespace);
	}

	public CloudFoundryAppDeployer getObject(CloudFoundryDeployerProperties props, String api, String org, String space, String email, String password, String namespace) {

		return this.cachedDeployers.computeIfAbsent(
				getKey(props, api, org, space, email, password, namespace),
				s -> doCreate(props, api, org, space, email, password));
	}

	private static CloudFoundryAppDeployer doCreate(CloudFoundryDeployerProperties props, String api, String org, String space, String email, String password) {

		final URL apiEndpoint;
		try {
			apiEndpoint = new URL(api);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		CloudFoundryClient client = SpringCloudFoundryClient.builder()
				.host(apiEndpoint.getHost())
				.port(apiEndpoint.getPort())
				.username(email)
				.password(password)
				.skipSslValidation(true)
				.build();

		CloudFoundryOperations operations = new CloudFoundryOperationsBuilder()
				.cloudFoundryClient(client)
				.target(org, space)
				.build();

		// TODO: Possibly utilize AppNameGenerator to generate appName.
		return new CloudFoundryAppDeployer(props, operations, client, appName -> appName);
	}

	private static String getKey(CloudFoundryDeployerProperties props, String api, String org, String space, String email, String password, String namespace) {
		return props.getBuildpack() + ":" + api + ":" + org + ":" + space + ":" + email + ":" + password + ":" + namespace;
	}

}
