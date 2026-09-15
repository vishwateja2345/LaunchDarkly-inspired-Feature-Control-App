package com.featureflags.history;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HistoryController {

	private final HistoryService historyService;

	public HistoryController(HistoryService historyService) {
		this.historyService = historyService;
	}

	@GetMapping("/api/v1/flags/{flagId}/history")
	public Map<String, Object> listForFlag(@PathVariable String flagId) {
		return Map.of("data", historyService.listForFlag(flagId).stream().map(AuditLogEntry::toMap).toList());
	}

	@GetMapping("/api/v1/projects/{projectId}/history")
	public Map<String, Object> listForProject(@PathVariable String projectId,
			@RequestParam(defaultValue = "50") int limit) {
		int bounded = Math.max(1, Math.min(limit, 200));

		return Map.of("data", historyService.listForProject(projectId, bounded).stream().map(AuditLogEntry::toMap).toList());
	}

	@GetMapping("/api/v1/history/{entryId}")
	public Map<String, Object> get(@PathVariable String entryId) {
		return Map.of("data", historyService.get(entryId).toMap());
	}
}
