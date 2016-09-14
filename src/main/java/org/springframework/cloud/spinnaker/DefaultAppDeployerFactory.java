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

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;

import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;

/**
 * @author Greg Turnquist
 */
public class DefaultAppDeployerFactory implements CloudFoundryAppDeployerFactory {

	private final Map<String, CloudFoundryAppDeployer> cachedDeployers = new HashMap<>();

	@Override
	public CloudFoundryAppDeployer getAppDeployer(URL apiEndpoint, String org, String space, String email, String password, String namespace) {

		return this.cachedDeployers.computeIfAbsent(
				getKey(apiEndpoint, org, space, email, password, namespace),
				s -> doCreate(apiEndpoint, org, space, email, password));
	}

	@Override
	public CloudFoundryClient getCloudFoundryClient(String email, String password, URL apiEndpoint) {
		ConnectionContext context = connectionContext(apiEndpoint);
		TokenProvider tokenProvider = tokenProvider(email, password);
		return doCreateCloudFoundryClient(context, tokenProvider);
	}

	@Override
	public CloudFoundryOperations getOperations(String email, String password, URL apiEndpoint, String org, String space) {
		CloudFoundryClient client = getCloudFoundryClient(email, password, apiEndpoint);
		ConnectionContext context = connectionContext(apiEndpoint);
		TokenProvider tokenProvider = tokenProvider(email, password);
		return doCreateOperations(client, context, tokenProvider, org, space);
	}

	private CloudFoundryAppDeployer doCreate(URL api, String org, String space, String email, String password) {

		CloudFoundryClient client = getCloudFoundryClient(email, password, api);
		CloudFoundryOperations operations = getOperations(email, password, api, org, space);

		return new CloudFoundryAppDeployer(
				new CloudFoundryConnectionProperties(),
				new CloudFoundryDeploymentProperties(),
				operations, client, appName -> appName);
	}

	private ConnectionContext connectionContext(URL apiEndpoint) {
		return DefaultConnectionContext.builder()
				.apiHost(apiEndpoint.getHost())
				.skipSslValidation(true)
				.sslHandshakeTimeout(Duration.ofSeconds(60))
				.build();
	}


	private TokenProvider tokenProvider(String username, String password) {
		return PasswordGrantTokenProvider.builder()
				.username(username)
				.password(password)
				.build();
	}


	private CloudFoundryClient doCreateCloudFoundryClient(ConnectionContext connectionContext, TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	private CloudFoundryOperations doCreateOperations(CloudFoundryClient cloudFoundryClient,
															 ConnectionContext connectionContext,
															 TokenProvider tokenProvider,
															 String org,
															 String space) {
		ReactorDopplerClient dopplerClient = ReactorDopplerClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();

		ReactorUaaClient uaaClient = ReactorUaaClient.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();

		return DefaultCloudFoundryOperations.builder()
				.cloudFoundryClient(cloudFoundryClient)
				.dopplerClient(dopplerClient)
				.uaaClient(uaaClient)
				.organization(org)
				.space(space)
				.build();
	}

	private String getKey(URL api, String org, String space, String email, String password, String namespace) {
		return api.toString() + ":" + org + ":" + space + ":" + email + ":" + password + ":" + namespace;
	}

}
