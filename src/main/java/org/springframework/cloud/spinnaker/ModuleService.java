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

import static java.util.stream.Stream.concat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.security.util.InMemoryResource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
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
	public static final String DEFAULT_PRIMARY_ACCOUNT = "prod";

	private final SpinnakerConfiguration spinnakerConfiguration;

	private final CloudFoundryAppDeployerFactory appDeployerFactory;

	private final ApplicationContext ctx;

	public ModuleService(SpinnakerConfiguration spinnakerConfiguration, CloudFoundryAppDeployerFactory appDeployerFactory, ApplicationContext ctx) {

		this.spinnakerConfiguration = spinnakerConfiguration;
		this.appDeployerFactory = appDeployerFactory;
		this.ctx = ctx;
	}

	/**
	 * Look up the status of all modules
	 *
	 * @return a {@link Stream} of {@link AppStatus}'s
	 */
	public Stream<AppStatus> getStatuses(String api, String org, String space, String email, String password, String namespace) {

		return spinnakerConfiguration.getModules().stream()
			.map(ModuleDetails::getName)
			.map(name -> appDeployerFactory.getObject(api, org, space, email, password, namespace).status(name + namespace));
	}

	/**
	 * Look up a single module's {@link AppStatus}
	 *
	 * @param name
	 * @return the {@link AppStatus} of the module
	 */
	public AppStatus getStatus(String name, String api, String org, String space, String email, String password, String namespace) {

		return lookupModule(name)
			.map(details -> details.getName())
			.map(moduleName -> appDeployerFactory.getObject(api, org, space, email, password, namespace).status(moduleName + namespace))
			.orElseThrow(handleNonExistentModule(name));
	}

	/**
	 * Deploy a module after finding its artifact.
	 *
	 * @param module
	 * @param data
	 * @throws IOException
	 */
	public void deploy(String module, Map<String, String> data, String api, String org, String space, String email, String password, String namespace) throws IOException {

		ModuleDetails details = getModuleDetails(module);

		final org.springframework.core.io.Resource artifactToDeploy = findArtifact(details, ctx, data);
		final Map<String, String> properties = getProperties(spinnakerConfiguration, details, data);

		log.debug("Uploading " + artifactToDeploy + "...");

		getCloudFoundryAppDeployer(details, api, org, space, email, password, namespace).deploy(new AppDeploymentRequest(
				new AppDefinition(details.getName() + namespace, Collections.emptyMap()),
				artifactToDeploy,
				properties
		));
	}

	/**
	 * Undeploy a module
	 *
	 * @param name
	 */
	public void undeploy(String name, String api, String org, String space, String email, String password, String namespace) {
		final CloudFoundryAppDeployer appDeployer = appDeployerFactory.getObject(api, org, space, email, password, namespace);
		appDeployer.undeploy(name);
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
						ApplicationContext ctx,
						Map<String, String> data) throws IOException {

		final String locationPattern = "classpath*:**/" + details.getArtifact() + "/**/" + details.getArtifact() + "-*.jar";
		final org.springframework.core.io.Resource[] resources = ctx.getResources(locationPattern);

		Assert.state(resources.length == 1, "Number of resources MUST be 1");

		log.info("Need to also chew on " + data);

		return (details.getName().equals("deck")
					? pluginSettingsJs(resources[0], data)
					: addConfigFile(details, resources[0], ctx));
	}

	/**
	 * Create an application deployer based on the module details
	 *
	 * TODO: Overhaul once buildpack is overrideable in the deployer.
	 *
	 * @param details
	 * @return
	 */
	private CloudFoundryAppDeployer getCloudFoundryAppDeployer(ModuleDetails details, String api, String org, String space, String email, String password, String namespace) {

		return Optional.ofNullable(details.getProperties().get("buildpack"))
			// TODO: Remove this step when Spring Cloud Deployer allows overriding the buildpack
			.map(buildpack -> mutateBuildpack(new CloudFoundryDeployerProperties(), buildpack))
			.map(props -> appDeployerFactory.getObject(props, api, org, space, email, password, namespace))
			.orElse(appDeployerFactory.getObject(api, org, space, email, password, namespace));
	}

	/**
	 * Clone the {@link CloudFoundryDeployerProperties} and alter the buildpack.
	 *
	 * TODO: Reevaluate after deployer updated to handle buildpack overrides.
	 *
	 * @param properties
	 * @param buildpack
	 * @return
	 */
	private static CloudFoundryDeployerProperties mutateBuildpack(CloudFoundryDeployerProperties properties, String buildpack) {

		CloudFoundryDeployerProperties clonedProps = cloneDeployerProperties(properties);
		clonedProps.setBuildpack(buildpack);
		return clonedProps;
	}

	/**
	 * Create a deep copy of {@link CloudFoundryDeployerProperties} to allow changes without affecting others.
	 *
	 * TODO: Reevaluate after deployer updated to handle buildpack overrides.
	 *
	 * @param properties
	 * @return a deep copy of {@link CloudFoundryDeployerProperties}
	 */
	private static CloudFoundryDeployerProperties cloneDeployerProperties(CloudFoundryDeployerProperties properties) {

		CloudFoundryDeployerProperties localProps = new CloudFoundryDeployerProperties();
		BeanUtils.copyProperties(properties, localProps);
		return localProps;
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
	private static Resource addConfigFile(ModuleDetails details, Resource resource, ApplicationContext ctx) {

		final ByteArrayOutputStream newJarByteStream = new ByteArrayOutputStream();

		try (
			ZipInputStream inputJarStream = new ZipInputStream(resource.getInputStream());
			ZipOutputStream newModuleJarFile = new ZipOutputStream(newJarByteStream)) {

			insertConfigFile(details, ctx, newModuleJarFile);

			ZipEntry entry;
			while ((entry = inputJarStream.getNextEntry()) != null) {
				passThroughFileEntry(inputJarStream, newModuleJarFile, entry);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			if (log.isDebugEnabled()) {
				Path file = Files.createTempFile(details.getName() + "-preview", ".jar");
				log.info("Dumping JAR contents to " + file);
				Files.write(file, newJarByteStream.toByteArray());
				file.toFile().deleteOnExit();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new InMemoryResource(newJarByteStream.toByteArray(), "In memory JAR file for " + details.getName());
	}

	private static void insertConfigFile(ModuleDetails details, ApplicationContext ctx, ZipOutputStream newModuleJarFile) throws IOException {
		JarEntry newEntry = new JarEntry(details.getName() + ".yml");
		newEntry.setTime(System.currentTimeMillis());
		newModuleJarFile.putNextEntry(newEntry);

		final String locationPattern = "classpath*:**/" + details.getArtifact() + "/config/" + details.getName() + ".yml";
		final Resource[] configFiles = ctx.getResources(locationPattern);

		Assert.state(configFiles.length == 1, "Number of resources MUST be 1");

		StreamUtils.copy(configFiles[0].getInputStream(), newModuleJarFile);

		newModuleJarFile.closeEntry();
	}


	private org.springframework.core.io.Resource pluginSettingsJs(org.springframework.core.io.Resource originalDeckJarFile, Map<String, String> data) {
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

			return new InMemoryResource(jarByteStream.toByteArray(), "In memory JAR file for deck");
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

}
