package com.featureflags.environments;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentController {

	private final EnvironmentService environmentService;

	public EnvironmentController(EnvironmentService environmentService) {
		this.environmentService = environmentService;
	}

	@GetMapping("/api/v1/projects/{projectId}/environments")
	public Map<String, Object> list(@PathVariable String projectId) {
		return Map.of("data", environmentService.listByProject(projectId).stream().map(Environment::toMap).toList());
	}

	@PostMapping("/api/v1/projects/{projectId}/environments")
	public ResponseEntity<Map<String, Object>> create(@PathVariable String projectId,
			@RequestBody(required = false) Map<String, Object> body) {
		Environment environment = environmentService.create(projectId, EnvironmentValidator.validateCreate(body));

		return ResponseEntity.status(201).body(Map.of("data", environment.toMap()));
	}

	@PatchMapping("/api/v1/environments/{environmentId}")
	public Map<String, Object> update(@PathVariable String environmentId,
			@RequestBody(required = false) Map<String, Object> body) {
		Environment environment = environmentService.update(environmentId, EnvironmentValidator.validateUpdate(body));

		return Map.of("data", environment.toMap());
	}

	@DeleteMapping("/api/v1/environments/{environmentId}")
	public ResponseEntity<Void> remove(@PathVariable String environmentId) {
		environmentService.remove(environmentId);

		return ResponseEntity.noContent().build();
	}
}
