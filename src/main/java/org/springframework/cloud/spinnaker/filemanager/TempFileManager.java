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
package org.springframework.cloud.spinnaker.filemanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.spinnaker.ModuleDetails;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * @author Greg Turnquist
 */
@Service
public class TempFileManager {

	private static final Logger log = LoggerFactory.getLogger(TempFileManager.class);

	final private Map<String, PathResource> cachedJars;

	public TempFileManager() {
		this.cachedJars = new HashMap<>();
	}


	public Resource createTempFile(ModuleDetails details, ByteArrayResource streamedArtifact) {

		return this.cachedJars.computeIfAbsent(getKey(details), key -> {

			Path path = null;
			try {
				path = Files.createTempFile(details.getArtifact(), ".jar");
				log.info("Dumping JAR contents to " + path);
				Files.write(path, streamedArtifact.getByteArray());
				path.toFile().deleteOnExit();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			return new PathResource(path);
		});
	}

	public void delete(ModuleDetails details) {
		this.cachedJars.computeIfPresent(getKey(details), (key, pathResource) -> {
			try {
				log.info("Deleting " + pathResource);
				pathResource.getFile().delete();
			} catch (IOException e) {
				log.error(e.getMessage());
			}
			return null;
		});
	}

	private String getKey(ModuleDetails details) {
		return details.getName();
	}
}
