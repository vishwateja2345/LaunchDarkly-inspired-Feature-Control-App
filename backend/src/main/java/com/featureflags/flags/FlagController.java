package com.featureflags.flags;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.featureflags.shared.RequestContext;

@RestController
public class FlagController {

	private final FlagService flagService;

	private final RequestContext requestContext;

	public FlagController(FlagService flagService, RequestContext requestContext) {
		this.flagService = flagService;
		this.requestContext = requestContext;
	}

	@GetMapping("/api/v1/projects/{projectId}/flags")
	public Map<String, Object> list(@PathVariable String projectId,
			@RequestParam(defaultValue = "false") boolean includeArchived) {
		return Map.of("data", flagService.list(projectId, includeArchived).stream().map(FeatureFlag::toMap).toList());
	}

	@PostMapping("/api/v1/projects/{projectId}/flags")
	public ResponseEntity<Map<String, Object>> create(@PathVariable String projectId,
			@RequestBody(required = false) Map<String, Object> body) {
		FeatureFlag flag = flagService.create(projectId, FlagValidator.validateCreate(body), requestContext.account());

		return ResponseEntity.status(201).body(Map.of("data", flag.toMap()));
	}

	@GetMapping("/api/v1/flags/{flagId}")
	public Map<String, Object> get(@PathVariable String flagId) {
		return Map.of("data", flagService.get(flagId).toMap());
	}

	@PatchMapping("/api/v1/flags/{flagId}")
	public Map<String, Object> update(@PathVariable String flagId,
			@RequestBody(required = false) Map<String, Object> body) {
		FeatureFlag flag = flagService.update(flagId, FlagValidator.validateUpdate(body), requestContext.account());

		return Map.of("data", flag.toMap());
	}

	@PostMapping("/api/v1/flags/{flagId}/archive")
	public Map<String, Object> archive(@PathVariable String flagId) {
		return Map.of("data", flagService.setArchived(flagId, true, requestContext.account()).toMap());
	}

	@PostMapping("/api/v1/flags/{flagId}/restore")
	public Map<String, Object> restore(@PathVariable String flagId) {
		return Map.of("data", flagService.setArchived(flagId, false, requestContext.account()).toMap());
	}
}
