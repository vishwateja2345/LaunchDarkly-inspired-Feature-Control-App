package com.featureflags.experiments;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExperimentController {

	private final ExperimentService experimentService;

	public ExperimentController(ExperimentService experimentService) {
		this.experimentService = experimentService;
	}

	@PostMapping("/api/v1/flags/{flagId}/environments/{environmentKey}/events/exposure")
	public ResponseEntity<Map<String, Object>> exposure(@PathVariable String flagId,
			@PathVariable String environmentKey, @RequestBody(required = false) Map<String, Object> body) {
		ExperimentEvent event = experimentService.recordExposure(flagId, environmentKey,
				ExperimentValidator.validateExposure(body));

		return ResponseEntity.status(201).body(Map.of("data", event.toMap()));
	}

	@PostMapping("/api/v1/flags/{flagId}/environments/{environmentKey}/events/conversion")
	public ResponseEntity<Map<String, Object>> conversion(@PathVariable String flagId,
			@PathVariable String environmentKey, @RequestBody(required = false) Map<String, Object> body) {
		ExperimentEvent event = experimentService.recordConversion(flagId, environmentKey,
				ExperimentValidator.validateConversion(body));

		return ResponseEntity.status(201).body(Map.of("data", event.toMap()));
	}

	@GetMapping("/api/v1/flags/{flagId}/environments/{environmentKey}/experiment/metrics")
	public Map<String, Object> metricKeys(@PathVariable String flagId, @PathVariable String environmentKey) {
		return Map.of("data", experimentService.metricKeys(flagId, environmentKey));
	}

	@GetMapping("/api/v1/flags/{flagId}/environments/{environmentKey}/experiment")
	public Map<String, Object> compare(@PathVariable String flagId, @PathVariable String environmentKey,
			@RequestParam(required = false) String metricKey) {
		return Map.of("data", experimentService.compare(flagId, environmentKey, metricKey));
	}
}
