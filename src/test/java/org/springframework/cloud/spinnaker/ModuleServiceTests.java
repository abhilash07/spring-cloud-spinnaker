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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.rules.ExpectedException.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.applications.InstanceDetail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppInstanceStatus;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.spinnaker.filemanager.TempFileManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Greg Turnquist
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ModuleServiceTests.TestConfig.class)
public class ModuleServiceTests {

	@Autowired
	TestAppDeployerFactory appDeployerFactory;

	@Autowired
	ModuleService moduleService;

	@Autowired
	MavenProperties mavenProperties;

	@Rule public ExpectedException thrown = none();

	@Test
	public void shouldReturnStatusCodeForRunningModules() throws Exception {

		// given
		CloudFoundryAppDeployer appDeployer = mock(CloudFoundryAppDeployer.class);
		appDeployerFactory.setStub(appDeployer);

		given(appDeployer.status("clouddriver")).willReturn(
			AppStatus
				.of("clouddriver")
				.with(
					new CloudFoundryAppInstanceStatus(
							ApplicationDetail.builder()
									.name("clouddriver")
									.id("abcdef")
									.stack("")
									.diskQuota(1024)
									.instances(1)
									.memoryLimit(1024)
									.requestedState("RUNNING")
									.runningInstances(1)
									.build(),
							InstanceDetail.builder()
									.state("RUNNING")
									.build(),
							0))
				.build());

		// when
		AppStatus status = moduleService.getStatus("clouddriver", new URL("http://api.example.com"), "org", "space", "user", "password", "");

		// then
		assertThat(status.getState(), equalTo(DeploymentState.deployed));

		then(appDeployer).should().status("clouddriver");
		verifyNoMoreInteractions(appDeployer);
	}

	@Test
	public void shouldReturnErrorForNonexistentModule() throws Exception {

		// given
		CloudFoundryAppDeployer appDeployer = mock(CloudFoundryAppDeployer.class);
		appDeployerFactory.setStub(appDeployer);

		given(appDeployer.status("nothing")).willReturn(
			AppStatus
				.of("nothing")
				.build());

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage(containsString("Module 'nothing' is not managed by this system"));

		// when
		moduleService.getStatus("nothing", new URL("http://api.example.com"), "org", "space", "user", "password", "");

		// then
		// JUnit exception conditions are at the top
	}

	@Test
	public void shouldHandleStandardDeployment() throws Exception {

		// given
		CloudFoundryAppDeployer appDeployer = mock(CloudFoundryAppDeployer.class);
		CloudFoundryClient client = mock(CloudFoundryClient.class);
		appDeployerFactory.setStub(appDeployer);
		appDeployerFactory.setStubClient(client);

		given(appDeployer.deploy(any())).willReturn("echo");
		given(appDeployer.status("echo")).willReturn(
				AppStatus
						.of("echo")
						.with(
								new CloudFoundryAppInstanceStatus(
										ApplicationDetail.builder()
												.name("echo")
												.id("abcdef")
												.stack("")
												.diskQuota(1024)
												.instances(1)
												.memoryLimit(1024)
												.requestedState("RUNNING")
												.runningInstances(1)
												.build(),
										InstanceDetail.builder()
												.state("RUNNING")
												.build(),
										0))
						.build());

		Resource artifactToUpload = mock(Resource.class);

		Map<String, String> data = new HashMap<>();
		data.put("foo", "bar");

		// when
		moduleService.deploy("echo", data, new URL("http://example.com"), "org", "space", "user", "password", "");

		// then
		then(appDeployer).should().deploy(new AppDeploymentRequest(
				new AppDefinition("echo", Collections.emptyMap()),
				artifactToUpload,
				any()
		));
		then(appDeployer).should().status("echo");
		verifyNoMoreInteractions(appDeployer);
	}

	@Test
	public void shouldHandlePrefixOverrides() throws IOException {

		// given
		CloudFoundryAppDeployer appDeployer = mock(CloudFoundryAppDeployer.class);
		CloudFoundryClient client = mock(CloudFoundryClient.class);
		appDeployerFactory.setStub(appDeployer);
		appDeployerFactory.setStubClient(client);

		given(appDeployer.deploy(any())).willReturn("echo-namespace");
		given(appDeployer.status("echo-namespace")).willReturn(
				AppStatus
						.of("echo-namespace")
						.with(
								new CloudFoundryAppInstanceStatus(
										ApplicationDetail.builder()
												.name("echo-namespace")
												.id("abcdef")
												.stack("")
												.diskQuota(1024)
												.instances(1)
												.memoryLimit(1024)
												.requestedState("RUNNING")
												.runningInstances(1)
												.build(),
										InstanceDetail.builder()
												.state("RUNNING")
												.build(),
										0))
						.build());

		Resource artifactToUpload = mock(Resource.class);

		Map<String, String> data = new HashMap<>();
		data.put("foo", "bar");

		// when
		moduleService.deploy("echo", data, new URL("http://example.com"), "org", "space", "user", "password", "namespace");

		// then
		then(appDeployer).should().deploy(new AppDeploymentRequest(
				new AppDefinition("echo-namespace", Collections.emptyMap()),
				artifactToUpload,
				any()
		));
		then(appDeployer).should().status("echo-namespace");
		verifyNoMoreInteractions(appDeployer);
	}

	@Test
	public void shouldHandleUndeployingAnApp() throws MalformedURLException {

		// given
		CloudFoundryClient client = mock(CloudFoundryClient.class);
		CloudFoundryOperations operations = mock(CloudFoundryOperations.class);
		CloudFoundryAppDeployer appDeployer = mock(CloudFoundryAppDeployer.class);
		appDeployerFactory.setStub(appDeployer);
		appDeployerFactory.setStubClient(client);
		appDeployerFactory.setStubOperations(operations);

		Applications applications = mock(Applications.class);

		given(operations.applications()).willReturn(applications);
		given(applications.delete(any())).willReturn(Mono.empty());

		// when
		moduleService.undeploy("clouddriver", new URL("http://example.com"), "org", "space", "user", "password", "");

		// then
		then(applications).should().delete(any());
		verifyNoMoreInteractions(appDeployer);
	}

	@Configuration
	@EnableConfigurationProperties(SpinnakerConfiguration.class)
	static class TestConfig {

		@Bean
		TestAppDeployerFactory cloudFoundryAppDeployerFactoryBean() {
			return new TestAppDeployerFactory();
		}

		@Bean
		TempFileManager fileManager() {
			return new TempFileManager();
		}

		@Bean
		ModuleService moduleService(SpinnakerConfiguration spinnakerConfiguration,
									CloudFoundryAppDeployerFactory appDeployerFactoryBean,
									ApplicationContext ctx,
									CounterService counterService,
									TempFileManager fileManager,
									MavenProperties mavenProperties) {
			return new ModuleService(spinnakerConfiguration, appDeployerFactoryBean, mockPatternResolver(ctx), counterService, fileManager, mavenProperties);
		}

		@Bean
		ResourcePatternResolver mockPatternResolver(ApplicationContext ctx) {
			ResourcePatternResolver mockResolver = mock(ResourcePatternResolver.class);
			try {
				when(mockResolver.getResources(any())).thenReturn(ctx.getResources("classpath:/echo-web-test.jar"));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return mockResolver;
		}

		@Bean
		CounterService counterService() {
			return mock(CounterService.class);
		}

		@Bean
		MavenProperties mavenProperties() throws IOException {

			MavenProperties properties = new MavenProperties();
			Path tempDirectory = Files.createTempDirectory("spring-cloud-spinnaker-tests-");
			tempDirectory.toFile().deleteOnExit();
			properties.setLocalRepository(tempDirectory.toString());
			properties.getRemoteRepositories().put("spring-snapshots", new MavenProperties.RemoteRepository("http://repo.spring.io/snapshot"));
			properties.getRemoteRepositories().put("spring-milestones", new MavenProperties.RemoteRepository("http://repo.spring.io/milestone"));
//			properties.getRemoteRepositories().put("spring-releases", new MavenProperties.RemoteRepository("http://repo.spring.io/release"));
			return properties;
		}

	}

}
