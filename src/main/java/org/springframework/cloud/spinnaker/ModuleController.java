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

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Greg Turnquist
 */
@RestController
public class ModuleController {

	private static final Logger log = LoggerFactory.getLogger(ModuleController.class);

	private final ModuleService moduleService;

	public ModuleController(ModuleService moduleService) {
		this.moduleService = moduleService;
	}

	@RequestMapping(method = RequestMethod.GET, value = ApiController.BASE_PATH + "/modules", produces = MediaTypes.HAL_JSON_VALUE)
	public ResponseEntity<?> statuses(@RequestParam("api") URL api,
									  @RequestParam("org") String org,
									  @RequestParam("space") String space,
									  @RequestParam("email") String email,
									  @RequestParam("password") String password,
									  @RequestParam(value = "namespace", defaultValue = "") String namespace) {

		return ResponseEntity.ok(new Resources<>(
			moduleService.getStatuses(api, org, space, email, password, namespace)
				.map(appStatus -> new Resource<>(
					appStatus,
					linkTo(methodOn(ModuleController.class).status(appStatus.getDeploymentId(), api, org, space, email, password, namespace)).withSelfRel(),
					linkTo(methodOn(ModuleController.class).start(appStatus.getDeploymentId(), api, org, space, email, password, namespace)).withRel("start"),
					linkTo(methodOn(ModuleController.class).stop(appStatus.getDeploymentId(), api, org, space, email, password, namespace)).withRel("stop")))
				.collect(Collectors.toList()),
				linkTo(methodOn(ModuleController.class).statuses(api, org, space, email, password, namespace)).withSelfRel()
		));
	}

	@RequestMapping(method = RequestMethod.GET, value = ApiController.BASE_PATH + "/modules/{module}", produces = MediaTypes.HAL_JSON_VALUE)
	public ResponseEntity<?> status(@PathVariable String module,
									@RequestParam("api") URL api,
									@RequestParam("org") String org,
									@RequestParam("space") String space,
									@RequestParam("email") String email,
									@RequestParam("password") String password,
									@RequestParam(value = "namespace", defaultValue = "") String namespace) {

		return ResponseEntity.ok(new Resource<>(
			moduleService.getStatus(module, api, org, space, email, password, namespace),
			linkTo(methodOn(ModuleController.class).status(module, api, org, space, email, password, namespace)).withSelfRel(),
			linkTo(methodOn(ModuleController.class).statuses(api, org, space, email, password, namespace)).withRel("all"),
			linkTo(methodOn(ApiController.class).root(api, org, space, email, password, namespace)).withRel("root"),
			linkTo(methodOn(ModuleController.class).start(module, api, org, space, email, password, namespace)).withRel("start"),
			linkTo(methodOn(ModuleController.class).stop(module, api, org, space, email, password, namespace)).withRel("stop")
		));
	}

	@RequestMapping(method = RequestMethod.POST, value = ApiController.BASE_PATH + "/modules/{module}")
	public ResponseEntity<?> deploy(@PathVariable String module,
									@RequestParam("api") URL api,
									@RequestParam("org") String org,
									@RequestParam("space") String space,
									@RequestParam("email") String email,
									@RequestParam("password") String password,
									@RequestParam(value = "namespace", defaultValue = "") String namespace,
									@RequestBody Map<String, String> data) throws IOException {

		moduleService.deploy(module, data, api, org, space, email, password, namespace);

		return ResponseEntity.created(linkTo(methodOn(ModuleController.class).status(module, api, org, space, email, password, namespace)).toUri()).build();
	}

	@RequestMapping(method = RequestMethod.POST, value = ApiController.BASE_PATH + "/modules/{module}/start")
	public ResponseEntity<?> start(@PathVariable String module,
								   @RequestParam("api") URL api,
								   @RequestParam("org") String org,
								   @RequestParam("space") String space,
								   @RequestParam("email") String email,
								   @RequestParam("password") String password,
								   @RequestParam(value = "namespace", defaultValue = "") String namespace) {

		moduleService.start(module, api, org, space, email, password, namespace);
		return ResponseEntity.noContent().build();
	}

	@RequestMapping(method = RequestMethod.POST, value = ApiController.BASE_PATH + "/modules/{module}/stop")
	public ResponseEntity<?> stop(@PathVariable String module,
								   @RequestParam("api") URL api,
								   @RequestParam("org") String org,
								   @RequestParam("space") String space,
								   @RequestParam("email") String email,
								   @RequestParam("password") String password,
								   @RequestParam(value = "namespace", defaultValue = "") String namespace) {

		moduleService.stop(module, api, org, space, email, password, namespace);
		return ResponseEntity.noContent().build();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = ApiController.BASE_PATH + "/modules/{module}")
	public ResponseEntity<?> undeploy(@PathVariable String module,
									  @RequestParam("api") URL api,
									  @RequestParam("org") String org,
									  @RequestParam("space") String space,
									  @RequestParam("email") String email,
									  @RequestParam("password") String password,
									  @RequestParam(value = "namespace", defaultValue = "") String namespace) {

		log.debug("Deleting " + module + " on the server...");

		moduleService.undeploy(module, api, org, space, email, password, namespace);

		return ResponseEntity.noContent().build();
	}

}
