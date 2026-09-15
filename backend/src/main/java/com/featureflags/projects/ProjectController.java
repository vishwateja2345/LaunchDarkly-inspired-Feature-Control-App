package com.featureflags.projects;

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
@RequestMapping("/api/v1/projects")
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	public Map<String, Object> list() {
		return Map.of("data", projectService.list().stream().map(Project::toMap).toList());
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> create(@RequestBody(required = false) Map<String, Object> body) {
		Project project = projectService.create(ProjectValidator.validateCreate(body));

		return ResponseEntity.status(201).body(Map.of("data", project.toMap()));
	}

	@GetMapping("/{projectId}")
	public Map<String, Object> get(@PathVariable String projectId) {
		return Map.of("data", projectService.get(projectId).toMap());
	}

	@PatchMapping("/{projectId}")
	public Map<String, Object> update(@PathVariable String projectId,
			@RequestBody(required = false) Map<String, Object> body) {
		Project project = projectService.update(projectId, ProjectValidator.validateUpdate(body));

		return Map.of("data", project.toMap());
	}

	@DeleteMapping("/{projectId}")
	public ResponseEntity<Void> remove(@PathVariable String projectId) {
		projectService.remove(projectId);

		return ResponseEntity.noContent().build();
	}
}
