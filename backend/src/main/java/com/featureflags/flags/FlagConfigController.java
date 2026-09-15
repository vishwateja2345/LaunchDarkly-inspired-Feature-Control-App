package com.featureflags.flags;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.history.AuditLogEntry;
import com.featureflags.history.HistoryService;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.RequestContext;
import com.featureflags.shared.Requests;

@RestController
public class FlagConfigController {

	private static final int NOT_FOUND = 404;

	private final FlagService flagService;

	private final FlagConfigService flagConfigService;

	private final EnvironmentService environmentService;

	private final HistoryService historyService;

	private final RequestContext requestContext;

	public FlagConfigController(FlagService flagService, FlagConfigService flagConfigService,
			EnvironmentService environmentService, HistoryService historyService, RequestContext requestContext) {
		this.flagService = flagService;
		this.flagConfigService = flagConfigService;
		this.environmentService = environmentService;
		this.historyService = historyService;
		this.requestContext = requestContext;
	}

	@GetMapping("/api/v1/flags/{flagId}/environments/{environmentKey}/config")
	public Map<String, Object> getConfig(@PathVariable String flagId, @PathVariable String environmentKey) {
		FeatureFlag flag = flagService.get(flagId);

		return Map.of("data", flagConfigService.getByEnvironmentKey(flag, environmentKey).toMap());
	}

	@PostMapping("/api/v1/flags/{flagId}/environments/{environmentKey}/restore")
	public Map<String, Object> restore(@PathVariable String flagId, @PathVariable String environmentKey,
			@RequestBody(required = false) Map<String, Object> body) {
		FeatureFlag flag = flagService.get(flagId);
		Environment environment = environmentService.requireByKey(flag.getProjectId(), environmentKey);
		String historyEntryId = Requests.string(Requests.body(body), "historyEntryId");

		if (historyEntryId == null) {
			throw new ApiException(400, "VALIDATION_ERROR", "historyEntryId is required.");
		}

		AuditLogEntry entry = historyService.get(historyEntryId);

		if (entry.getAfterSnapshot() == null || !flagId.equals(entry.getFlagId())
				|| !environment.getId().equals(entry.getEnvironmentId())) {
			throw new ApiException(NOT_FOUND, "HISTORY_ENTRY_NOT_FOUND",
					"That history entry does not have a restorable snapshot for this flag and environment.");
		}

		FlagConfigValidator.ParsedChange change = FlagConfigValidator.validate(flag, entry.getAfterSnapshot());
		FlagEnvironmentConfig restored = flagConfigService.applyDirect(flag, environment, change,
				requestContext.account(), "ROLLED_BACK",
				requestContext.account().getName() + " restored the " + environment.getName()
						+ " configuration from " + entry.getCreatedAt() + ".");

		return Map.of("data", restored.toMap());
	}
}
