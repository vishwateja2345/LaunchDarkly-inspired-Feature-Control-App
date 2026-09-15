package com.featureflags.audiences;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppUserController {

	private final AppUserService appUserService;

	public AppUserController(AppUserService appUserService) {
		this.appUserService = appUserService;
	}

	@GetMapping("/api/v1/projects/{projectId}/users")
	public Map<String, Object> list(@PathVariable String projectId,
			@RequestParam(required = false) String search) {
		return Map.of("data", appUserService.list(projectId, search).stream().map(AppUser::toMap).toList());
	}

	@PostMapping("/api/v1/projects/{projectId}/users")
	public ResponseEntity<Map<String, Object>> create(@PathVariable String projectId,
			@RequestBody(required = false) Map<String, Object> body) {
		AppUser user = appUserService.create(projectId, AppUserValidator.validateCreate(body));

		return ResponseEntity.status(201).body(Map.of("data", user.toMap()));
	}

	@PatchMapping("/api/v1/users/{userId}")
	public Map<String, Object> update(@PathVariable String userId,
			@RequestBody(required = false) Map<String, Object> body) {
		AppUser user = appUserService.update(userId, AppUserValidator.validateUpdate(body));

		return Map.of("data", user.toMap());
	}

	@DeleteMapping("/api/v1/users/{userId}")
	public ResponseEntity<Void> remove(@PathVariable String userId) {
		appUserService.remove(userId);

		return ResponseEntity.noContent().build();
	}
}
