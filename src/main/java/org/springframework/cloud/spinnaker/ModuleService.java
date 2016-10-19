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

import static java.util.stream.Stream.*;
import static org.cloudfoundry.util.DelayUtils.*;
import static org.cloudfoundry.util.tuple.TupleUtils.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.spinnaker.filemanager.TempFileManager;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * A service to handle module level operations.
 *
 * @author Greg Turnquist
 */
@Service
public class ModuleService {

	private static final Logger log = LoggerFactory.getLogger(ModuleService.class);

	public static final String DEFAULT_DOMAIN = "cfapps.io"; // PWS
	public static final String DEFAULT_PRIMARY_ACCOUNT = "production";

	// Metrics
	private static final String METRICS_DEPLOYED = "spinnaker.deployment.%s.deployed";
	private static final String METRICS_UNDEPLOYED = "spinnaker.deployment.%s.undeployed";

	private final SpinnakerConfiguration spinnakerConfiguration;

	private final CloudFoundryAppDeployerFactory appDeployerFactory;

	private final ResourcePatternResolver ctx;

	private final CounterService counterService;

	private final TempFileManager fileManager;

	private final MavenProperties mavenProperties;

	public ModuleService(SpinnakerConfiguration spinnakerConfiguration,
						 CloudFoundryAppDeployerFactory appDeployerFactory,
						 ResourcePatternResolver ctx,
						 CounterService counterService,
						 TempFileManager fileManager,
						 MavenProperties mavenProperties) {

		this.spinnakerConfiguration = spinnakerConfiguration;
		this.appDeployerFactory = appDeployerFactory;
		this.ctx = ctx;
		this.counterService = counterService;
		this.fileManager = fileManager;
		this.mavenProperties = mavenProperties;
	}

	/**
	 * Look up the status of all modules
	 *
	 * @return a {@link Stream} of {@link AppStatus}'s
	 */
	public Stream<AppStatus> getStatuses(URL apiEndpoint, String org, String space, String email, String password, String namespace) {

		return spinnakerConfiguration.getModules().stream()
			.map(ModuleDetails::getName)
			.map(name -> appDeployerFactory.getAppDeployer(apiEndpoint, org, space, email, password, namespace).status(name + namespace));
	}

	/**
	 * Look up a single module's {@link AppStatus}
	 *
	 * @param name
	 * @return the {@link AppStatus} of the module
	 */
	public AppStatus getStatus(String name, URL api, String org, String space, String email, String password, String namespace) {

		return lookupModule(name)
			.map(details -> details.getName())
			.map(moduleName -> appDeployerFactory.getAppDeployer(api, org, space, email, password, namespace).status(moduleName + namespace))
			.orElseThrow(handleNonExistentModule(name));
	}

	/**
	 * Deploy a module after finding its artifact.
	 *
	 * @param module
	 * @param data
	 * @throws IOException
	 */
	public void deploy(String module, Map<String, String> data, URL apiEndpoint, String org, String space, String email, String password, String namespace) throws IOException {

		ModuleDetails details = getModuleDetails(module);

		final Resource artifactToDeploy = findArtifact(details, ctx, data);

		final Map<String, String> properties = getProperties(spinnakerConfiguration, details, data);

		final Map<String, String> deploymentProperties = new HashMap<>();
		deploymentProperties.put(CloudFoundryDeploymentProperties.USE_SPRING_APPLICATION_JSON_KEY, "false");
		deploymentProperties.put(
				CloudFoundryDeploymentProperties.SERVICES_PROPERTY_KEY,
				Stream.concat(
						details.getServices().stream(),
						StringUtils.commaDelimitedListToSet(
								data.getOrDefault(CloudFoundryDeploymentProperties.SERVICES_PROPERTY_KEY, "")).stream())
					.collect(Collectors.joining(",")));

		Optional.ofNullable(details.getProperties().get("buildpack"))
				.ifPresent(buildpack -> deploymentProperties.put(CloudFoundryDeploymentProperties.BUILDPACK_PROPERTY_KEY, buildpack));

		Optional.ofNullable(details.getProperties().get("memory"))
			.ifPresent(memory -> deploymentProperties.put(CloudFoundryDeploymentProperties.MEMORY_PROPERTY_KEY, memory));

		Optional.ofNullable(details.getProperties().get("disk"))
			.ifPresent(disk -> deploymentProperties.put(CloudFoundryDeploymentProperties.DISK_PROPERTY_KEY, disk));

		CloudFoundryAppDeployer appDeployer = appDeployerFactory.getAppDeployer(apiEndpoint, org, space, email, password, namespace);

		log.debug("Uploading " + artifactToDeploy + "...");

		String deploymentId = appDeployer.deploy(new AppDeploymentRequest(
				new AppDefinition(details.getName() + namespace, properties),
				artifactToDeploy,
				deploymentProperties));

		try {
			Mono.defer(() -> Mono.just(appDeployer.status(deploymentId)))
				.filter(appStatus ->
					appStatus.getState() == DeploymentState.deployed || appStatus.getState() == DeploymentState.deploying)
				.repeatWhenEmpty(exponentialBackOff(Duration.ofSeconds(1), Duration.ofSeconds(15), Duration.ofMinutes(15)))
				.block(Duration.ofMinutes(20));
		} finally {
			this.fileManager.delete(details);
		}

		counterService.increment(String.format(METRICS_DEPLOYED, module));
	}

	/**
	 * Undeploy a module
	 *
	 * @param name
	 */
	public void undeploy(String name, URL apiEndpoint, String org, String space, String email, String password, String namespace) {

		CloudFoundryOperations operations = appDeployerFactory.getOperations(email, password, apiEndpoint, org, space);

		// TODO: Migrate to new SPI with reactive interface.
		//appDeployer.undeploy(name);

		operations.applications()
			.delete(DeleteApplicationRequest.builder()
				.name(name)
				.deleteRoutes(true)
				.build())
			.block(Duration.ofMinutes(5));

		counterService.increment(String.format(METRICS_UNDEPLOYED, name));
	}

	/**
	 * Start a given module on the CF
	 *
	 * @param name
	 */
	public void start(String name, URL apiEndpoint, String org, String space, String email, String password, String namespace) {

		CloudFoundryOperations operations = appDeployerFactory.getOperations(email, password, apiEndpoint, org, space);

		CloudFoundryAppDeployer appDeployer = appDeployerFactory.getAppDeployer(apiEndpoint, org, space, email, password, namespace);

		operations.applications()
			.start(StartApplicationRequest.builder()
				.name(name)
				.build())
			.then(() -> Mono.defer(() -> Mono.just(appDeployer.status(name)))
				.filter(appStatus ->
					appStatus.getState() == DeploymentState.deployed || appStatus.getState() == DeploymentState.failed)
				.repeatWhenEmpty(exponentialBackOff(Duration.ofSeconds(1), Duration.ofSeconds(15), Duration.ofMinutes(15)))
				.doOnSuccess(v -> log.debug(String.format("Successfully started %s", name)))
				.doOnError(e -> log.error(String.format("Unable to start %s", name))))
			.block(Duration.ofMinutes(10));
	}

	/**
	 * Start a given module on the CF
	 *
	 * @param name
	 */
	public void stop(String name, URL apiEndpoint, String org, String space, String email, String password, String namespace) {

		CloudFoundryOperations operations = appDeployerFactory.getOperations(email, password, apiEndpoint, org, space);

		CloudFoundryAppDeployer appDeployer = appDeployerFactory.getAppDeployer(apiEndpoint, org, space, email, password, namespace);

		operations.applications()
			.stop(StopApplicationRequest.builder()
				.name(name)
				.build())
			.then(() -> Mono.defer(() -> Mono.just(appDeployer.status(name)))
				.filter(appStatus -> appStatus.getState() == DeploymentState.unknown)
				.repeatWhenEmpty(exponentialBackOff(Duration.ofSeconds(1), Duration.ofSeconds(15), Duration.ofMinutes(15)))
				.doOnSuccess(v -> log.debug(String.format("Successfully stopped %s", name)))
				.doOnError(e -> log.error(String.format("Unable to stop %s", name))))
			.block(Duration.ofSeconds(30));
	}

	/**
	 * Lookup if a module exists in the configuration settings.
	 *
	 * @param name
	 * @return {@link Optional} name of the module.
	 */
	private Optional<ModuleDetails> lookupModule(String name) {

		return spinnakerConfiguration.getModules().stream()
			.filter(details -> name.startsWith(details.getName()))
			.findAny();
	}


	/**
	 * Look up a given module from it's configuration properties listing
	 *
	 * @param module
	 * @return
	 */
	private ModuleDetails getModuleDetails(String module) {

		return lookupModule(module)
				.map(moduleDetails -> moduleDetails)
				.orElseThrow(handleNonExistentModule(module));
	}

	private static Supplier<IllegalArgumentException> handleNonExistentModule(String module) {
		return () -> new IllegalArgumentException("Module '" + module + "' is not managed by this system");
	}

	private Resource findArtifact(ModuleDetails details,
								  ResourcePatternResolver ctx,
								  Map<String, String> data) throws IOException {

		if (details.getName().equals("deck")) {
			return this.fileManager.createTempFile(details, findDeckMavenArtifact(details, data));
		} else if (Arrays.asList("clouddriver", "echo", "front50").contains(details.getName())) {
			return this.fileManager.createTempFile(details, findMavenArtifact(details));
		} else {
			ByteArrayResource streamedArtifact = Stream.of(ctx.getResources(
				"classpath*:**/" + details.getArtifact() + "/**/" + details.getArtifact() + "-*.jar"))
				.findFirst()
				.map(resource -> {
					try {
						log.info("Deploying " + resource.getFilename() + " found at " + resource.getURI().toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
					log.info("Need to also chew on " + data);

					return addConfigFile(details, resource, ctx);
				})
				.orElseThrow(() -> new RuntimeException("Unable to find artifact for " + details.getArtifact()));

			return this.fileManager.createTempFile(details, streamedArtifact);
		}
	}

	/**
	 * Pull deck, unpack it, and insert a templated settings.js file based on user settings.
	 *
	 * @param details
	 * @param data
	 * @return
	 */
	private ByteArrayResource findDeckMavenArtifact(ModuleDetails details,
										   Map<String, String> data) {

		log.info("Fetching " + details.getArtifact() + " from the web...");
		MavenResource mavenResource = MavenResource.parse(details.getArtifact(), this.mavenProperties);

		ByteArrayOutputStream newDeck = new ByteArrayOutputStream();

		try {
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

			try (ZipFile deckInputFile = new ZipFile(mavenResource.getFile())) {
				try (JarOutputStream deckOutputStream = new JarOutputStream(newDeck, manifest)) {

					deckInputFile.stream()
						.filter(e -> e.getName().startsWith("META-INF/resources/"))
						.map(e -> {
							JarEntry newEntry = new JarEntry(e.getName().substring("META-INF/resources/".length()));
							newEntry.setTime(System.currentTimeMillis());
							try {
								return Tuples.of(deckInputFile.getInputStream(e), e.isDirectory(), newEntry);
							} catch (IOException e1) {
								throw new RuntimeException(e1);
							}
						})
						.filter(predicate((entryStream, isDirectory, newEntry) -> !newEntry.getName().equals("")))
						.map(function((entryStream, isDirectory, newEntry) -> {
							if (newEntry.getName().equals("settings.js")) {
								try {

									String originalSettingsJs = StreamUtils.copyToString(entryStream, Charset.defaultCharset());

									int start = originalSettingsJs.indexOf("'use strict'");
									int end = originalSettingsJs.lastIndexOf("/* WEBPACK VAR INJECTION */");

									String customSettingsJs = StreamUtils.copyToString(ctx.getResource("classpath:settings.js").getInputStream(), Charset.defaultCharset());

									customSettingsJs = customSettingsJs.replace("{gate}", "https://gate" + data.getOrDefault("namespace", "") + "." + data.getOrDefault("deck.domain", DEFAULT_DOMAIN));
									customSettingsJs = customSettingsJs.replace("{primaryAccount}", data.getOrDefault("deck.primaryAccount", DEFAULT_PRIMARY_ACCOUNT));
									customSettingsJs = customSettingsJs.replace("{defaultOrg}", data.getOrDefault("providers.cf.defaultOrg", ""));
									customSettingsJs = customSettingsJs.replace("'{primaryAccounts}'", "[" + StringUtils.collectionToCommaDelimitedString(
										Arrays.stream(data.getOrDefault("deck.primaryAccounts", DEFAULT_PRIMARY_ACCOUNT).split(","))
											.map(account -> "'" + account + "'")
											.collect(Collectors.toList())) + "]");

									InputStream modifiedStream = new ByteArrayInputStream((
										originalSettingsJs.substring(0, start) +
										customSettingsJs +
										originalSettingsJs.substring(end)).getBytes());

									return Tuples.of(modifiedStream, isDirectory, newEntry);
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
							} else {
								return Tuples.of(entryStream, isDirectory, newEntry);
							}
						}))
						.forEach(consumer((entryStream, isDirectory, newEntry) -> {
							try {
								deckOutputStream.putNextEntry(newEntry);
								if (!isDirectory) {
									StreamUtils.copy(entryStream, deckOutputStream);
								}
								deckOutputStream.closeEntry();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new ByteArrayResource(newDeck.toByteArray());
	}

	/**
	 * Fetch an artifact from one of the maven repositories listed in the properties.
	 * Convert it into a {@link ByteArrayResource}.
	 *
	 * @param details
	 * @return
	 */
	private ByteArrayResource findMavenArtifact(ModuleDetails details) {

		log.info("Fetching " + details.getArtifact() + " from the web...");
		MavenResource mavenResource = MavenResource.parse(details.getArtifact(), this.mavenProperties);

		ByteArrayOutputStream downloadedArtifact = new ByteArrayOutputStream();

		try {
			StreamUtils.copy(mavenResource.getInputStream(), downloadedArtifact);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new ByteArrayResource(downloadedArtifact.toByteArray());

	}

	/**
	 * Merge top level properties and module-specific ones.
	 *
	 * @param details
	 * @param data
	 * @return
	 */
	private static Map<String, String> getProperties(SpinnakerConfiguration spinnakerConfiguration, ModuleDetails details, Map<String, String> data) {

		final Map<String, String> properties = concat(
				spinnakerConfiguration.getProperties().entrySet().stream(),
				details.getProperties().entrySet().stream()
		).collect(Collectors.toMap(
				e -> e.getKey().replace("&lsq;", "[").replace("&rsq;", "]"),
				e -> e.getValue().replace("${module}", details.getName()),
				(a, b) -> b));

		data.entrySet().stream()
				.forEach(entry -> properties.put(entry.getKey(), entry.getValue()));

		return properties;
	}

	/**
	 * While copying in a module's JAR, add it's related application.yml file
	 *
	 * @param details
	 * @param resource
	 * @return
	 */
	private static ByteArrayResource addConfigFile(ModuleDetails details, Resource resource, ResourcePatternResolver ctx) {

		final ByteArrayOutputStream newJarByteStream = new ByteArrayOutputStream();

		try (
			ZipInputStream inputJarStream = new ZipInputStream(resource.getInputStream());
			ZipOutputStream newModuleJarFile = new ZipOutputStream(newJarByteStream)) {

			insertConfigFile(details, ctx, newModuleJarFile);
			insertExtraConfigFiles(details, ctx, newModuleJarFile);

			ZipEntry entry;
			while ((entry = inputJarStream.getNextEntry()) != null) {
				passThroughFileEntry(inputJarStream, newModuleJarFile, entry);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new ByteArrayResource(newJarByteStream.toByteArray(), "In memory JAR file for " + details.getName());
	}

	private static void insertConfigFile(ModuleDetails details, ResourcePatternResolver ctx, ZipOutputStream newModuleJarFile) throws IOException {
		JarEntry newEntry = new JarEntry(details.getName() + ".yml");
		newEntry.setTime(System.currentTimeMillis());
		newModuleJarFile.putNextEntry(newEntry);

		final String locationPattern = "classpath*:**/" + details.getArtifact() + "/config/" + details.getName() + ".yml";
		final Resource[] configFiles = ctx.getResources(locationPattern);

		Stream.of(configFiles)
				.findFirst()
				.ifPresent(resource -> {
					try {
						log.info("Deploying " + resource.getFilename() + " found at " + resource.getURI().toString());
						StreamUtils.copy(resource.getInputStream(), newModuleJarFile);
						newModuleJarFile.closeEntry();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
	}

	private static void insertExtraConfigFiles(ModuleDetails details, ResourcePatternResolver ctx, ZipOutputStream newModuleJarFile) throws IOException {

		final String locationPattern = "classpath*:**/" + details.getName() + "-*.yml";
		final Resource[] configFiles = ctx.getResources(locationPattern);
		Stream.of(configFiles)
			.forEach(configFile -> {
				try {
					log.info("Adding " + configFile.getFilename() + " to " + details.getName() + " found at " + configFile.getURI().toString());
					JarEntry newEntry = new JarEntry(configFile.getFilename());
					newEntry.setTime(System.currentTimeMillis());
					newModuleJarFile.putNextEntry(newEntry);
					StreamUtils.copy(configFile.getInputStream(), newModuleJarFile);
					newModuleJarFile.closeEntry();
				} catch (IOException e) {
					log.warn("Unable to process " + configFile.getFilename() + " => " + e.getMessage());
				}
			});
	}

	private static void passThroughFileEntry(ZipInputStream zipInputStream, ZipOutputStream newJarFile, ZipEntry entry) throws IOException {
		JarEntry newEntry = new JarEntry(entry);
		newJarFile.putNextEntry(newEntry);
		if (!entry.isDirectory()) {
			StreamUtils.copy(zipInputStream, newJarFile);
		}
		newJarFile.closeEntry();
	}

}
