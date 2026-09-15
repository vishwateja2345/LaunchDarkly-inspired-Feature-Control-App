package com.featureflags.audiences;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SegmentController {

	private final SegmentService segmentService;

	public SegmentController(SegmentService segmentService) {
		this.segmentService = segmentService;
	}

	@GetMapping("/api/v1/projects/{projectId}/segments")
	public Map<String, Object> list(@PathVariable String projectId) {
		return Map.of("data", segmentService.list(projectId).stream().map(Segment::toMap).toList());
	}

	@PostMapping("/api/v1/projects/{projectId}/segments")
	public ResponseEntity<Map<String, Object>> create(@PathVariable String projectId,
			@RequestBody(required = false) Map<String, Object> body) {
		Segment segment = segmentService.create(projectId, SegmentValidator.validate(body, true));

		return ResponseEntity.status(201).body(Map.of("data", segment.toMap()));
	}

	@GetMapping("/api/v1/segments/{segmentId}")
	public Map<String, Object> get(@PathVariable String segmentId) {
		return Map.of("data", segmentService.get(segmentId).toMap());
	}

	@PutMapping("/api/v1/segments/{segmentId}")
	public Map<String, Object> update(@PathVariable String segmentId,
			@RequestBody(required = false) Map<String, Object> body) {
		Segment segment = segmentService.update(segmentId, SegmentValidator.validate(body, false));

		return Map.of("data", segment.toMap());
	}

	@DeleteMapping("/api/v1/segments/{segmentId}")
	public ResponseEntity<Void> remove(@PathVariable String segmentId) {
		segmentService.remove(segmentId);

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/api/v1/segments/{segmentId}/preview")
	public Map<String, Object> preview(@PathVariable String segmentId) {
		return Map.of("data", Map.of("matchingUserCount", segmentService.previewMatchCount(segmentId)));
	}
}
