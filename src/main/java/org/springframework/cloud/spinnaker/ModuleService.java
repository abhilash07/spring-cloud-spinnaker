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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.applications.StopApplicationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.metrics.CounterService;
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

	public ModuleService(SpinnakerConfiguration spinnakerConfiguration,
						 CloudFoundryAppDeployerFactory appDeployerFactory,
						 ResourcePatternResolver ctx,
						 CounterService counterService,
						 TempFileManager fileManager) {

		this.spinnakerConfiguration = spinnakerConfiguration;
		this.appDeployerFactory = appDeployerFactory;
		this.ctx = ctx;
		this.counterService = counterService;
		this.fileManager = fileManager;
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
				.block(Duration.ofMinutes(10));
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

		final String locationPattern = "classpath*:**/" + details.getArtifact() + "/**/" + details.getArtifact() + "-*.jar";
		final org.springframework.core.io.Resource[] resources = ctx.getResources(locationPattern);

		ByteArrayResource streamedArtifact = Stream.of(resources)
				.findFirst()
				.map(resource -> {
					try {
						log.info("Deploying " + resource.getFilename() + " found at " + resource.getURI().toString());
					} catch (IOException e) {
						e.printStackTrace();
					}
					log.info("Need to also chew on " + data);

					return (details.getName().equals("deck")
							? pluginSettingsJs(resources[0], data)
							: addConfigFile(details, resources[0], ctx));
				})
				.orElseThrow(() -> new RuntimeException("Unable to find artifact for " + details.getArtifact()));

		return this.fileManager.createTempFile(details, streamedArtifact);
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

//		try {
//			if (log.isDebugEnabled()) {
//				Path file = Files.createTempFile(details.getName() + "-preview", ".jar");
//				log.info("Dumping JAR contents to " + file);
//				Files.write(file, newJarByteStream.toByteArray());
//				file.toFile().deleteOnExit();
//			}
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

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


	private ByteArrayResource pluginSettingsJs(Resource originalDeckJarFile, Map<String, String> data) {
		try {
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
			final ByteArrayOutputStream jarByteStream = new ByteArrayOutputStream();

			try (
					ZipInputStream inputJarStream = new ZipInputStream(originalDeckJarFile.getInputStream());
					JarOutputStream newDeckJarFile = new JarOutputStream(jarByteStream, manifest)
			) {
				ZipEntry entry;
				while ((entry = inputJarStream.getNextEntry()) != null) {
					try {

						if (entry.getName().contains("META-INF") || entry.getName().contains("MANIFEST.MF")) {
							// Skip the manifest since it's set up above.
						} else if (entry.getName().equals("settings.js")) {
							transformSettingsJs(data, inputJarStream, newDeckJarFile, entry);
						} else {
							passThroughFileEntry(inputJarStream, newDeckJarFile, entry);
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				};
			}

//			if (log.isDebugEnabled()) {
//				Path file = Files.createTempFile("deck-preview", ".jar");
//				log.info("Dumping JAR contents to " + file);
//				Files.write(file, jarByteStream.toByteArray());
//				file.toFile().deleteOnExit();
//			}

			return new ByteArrayResource(jarByteStream.toByteArray(), "In memory JAR file for deck");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private static void transformSettingsJs(Map<String, String> data, ZipInputStream zipInputStream, JarOutputStream newDeckJarFile, ZipEntry entry) throws IOException {
		JarEntry newEntry = new JarEntry(entry.getName());
		newEntry.setTime(entry.getTime());
		newDeckJarFile.putNextEntry(newEntry);
		if (!entry.isDirectory()) {
			String settingsJs = StreamUtils.copyToString(zipInputStream, Charset.defaultCharset());;
			settingsJs = settingsJs.replace("{gate}", "https://gate" + data.getOrDefault("namespace", "") + "." + data.getOrDefault("deck.domain", DEFAULT_DOMAIN));
			settingsJs = settingsJs.replace("{primaryAccount}", data.getOrDefault("deck.primaryAccount", DEFAULT_PRIMARY_ACCOUNT));
			settingsJs = settingsJs.replace("{defaultOrg}", data.getOrDefault("providers.cf.defaultOrg", ""));
			final String primaryAccounts = data.getOrDefault("deck.primaryAccounts", DEFAULT_PRIMARY_ACCOUNT);
			final String[] primaryAccountsArray = primaryAccounts.split(",");
			final List<String> accounts = Arrays.stream(primaryAccountsArray)
					.map(account -> "'" + account + "'")
					.collect(Collectors.toList());
			final String formattedAccounts = StringUtils.collectionToCommaDelimitedString(accounts);
			settingsJs = settingsJs.replace("'{primaryAccounts}'", "[" + formattedAccounts + "]");
			StreamUtils.copy(settingsJs, Charset.defaultCharset(), newDeckJarFile);
		}
		newDeckJarFile.closeEntry();
	}

	private static void passThroughFileEntry(ZipInputStream zipInputStream, ZipOutputStream newJarFile, ZipEntry entry) throws IOException {
		JarEntry newEntry = new JarEntry(entry);
		newJarFile.putNextEntry(newEntry);
		if (!entry.isDirectory()) {
			StreamUtils.copy(zipInputStream, newJarFile);
		}
		newJarFile.closeEntry();
	}

	private static BiFunction<String, String, String> collectStates() {
		return (totalState, instanceState) -> {
			log.info("Total state = " + totalState + " Instance state = " + instanceState);
			if ("RUNNING".equals(instanceState) || "RUNNING".equals(totalState)) {
				return "RUNNING";
			}

			if ("FLAPPING".equals(instanceState) || "CRASHED".equals(instanceState)) {
				return "FAILED";
			}

			return totalState;
		};
	}

}
