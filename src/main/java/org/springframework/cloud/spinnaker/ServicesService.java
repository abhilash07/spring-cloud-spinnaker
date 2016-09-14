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
import java.util.List;
import java.util.Optional;

import org.cloudfoundry.operations.services.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/**
 * Service to look up Cloud Foundry Services (funny, ehh?)
 *
 * @author Greg Turnquist
 */
@Service
public class ServicesService {

	private static final Logger log = LoggerFactory.getLogger(ServicesService.class);

	private final CloudFoundryAppDeployerFactory appDeployerFactory;

	public ServicesService(CloudFoundryAppDeployerFactory appDeployerFactory) {
		this.appDeployerFactory = appDeployerFactory;
	}

	public List<ServiceInstance> getServices(String serviceType, String email, String password, URL apiEndpoint, String org, String space) {

		String token = Optional.ofNullable(serviceType)
			.map(String::toLowerCase)
			.orElse("");

		return appDeployerFactory.getOperations(email, password, apiEndpoint, org, space)
			.services()
				.listInstances()
				.filter(serviceInstance ->
					Optional.ofNullable(serviceInstance.getDescription()).map(String::toLowerCase).orElse("")
						.contains(token)
						||
						Optional.ofNullable(serviceInstance.getService()).map(String::toLowerCase).orElse("")
							.contains(token))
				.collectList()
				.block(Duration.ofSeconds(60));
	}

}
