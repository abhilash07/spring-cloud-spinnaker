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

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.spinnaker.filemanager.TempFileManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Greg Turnquist
 */
@Configuration
@EnableConfigurationProperties({SpinnakerConfiguration.class})
public class Config {

	@Bean
	ModuleService moduleService(SpinnakerConfiguration spinnakerConfiguration,
								CloudFoundryAppDeployerFactory appDeployerFactoryBean,
								ApplicationContext ctx,
								CounterService counterService,
								TempFileManager fileManager,
								MavenProperties mavenProperties) {
		return new ModuleService(spinnakerConfiguration, appDeployerFactoryBean, ctx, counterService, fileManager, mavenProperties);
	}

	@Bean
	CloudFoundryAppDeployerFactory cloudFoundryAppDeployerFactoryBean() {
		return new DefaultAppDeployerFactory();
	}

	@Bean
	MavenProperties mavenProperties() {

		MavenProperties properties = new MavenProperties();
		properties.getRemoteRepositories().put("jcenter", new MavenProperties.RemoteRepository("http://jcenter.bintray.com/"));
		properties.getRemoteRepositories().put("spring-snapshots", new MavenProperties.RemoteRepository("http://repo.spring.io/snapshot"));
		properties.getRemoteRepositories().put("spring-milestones", new MavenProperties.RemoteRepository("http://repo.spring.io/milestone"));
		properties.getRemoteRepositories().put("spring-releases", new MavenProperties.RemoteRepository("http://repo.spring.io/release"));
		return properties;
	}

}
