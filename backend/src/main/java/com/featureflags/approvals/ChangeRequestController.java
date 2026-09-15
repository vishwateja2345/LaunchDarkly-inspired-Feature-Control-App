package com.featureflags.approvals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.flags.FeatureFlag;
import com.featureflags.flags.FlagConfigService;
import com.featureflags.flags.FlagConfigValidator;
import com.featureflags.flags.FlagEnvironmentConfig;
import com.featureflags.flags.FlagService;
import com.featureflags.history.AuditLogEntry;
import com.featureflags.history.HistoryService;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.RequestContext;
import com.featureflags.shared.Requests;

/**
 * The single gateway for changing what a flag serves in an environment. Production
 * environments always require a second person's approval; every other environment
 * applies immediately. The quick on/off toggle, the full targeting editor, and
 * restoring a past version from history all submit through here so there is
 * exactly one code path enforcing that rule.
 */
@RestController
public class ChangeRequestController {

	private static final int NOT_FOUND = 404;

	private final FlagService flagService;

	private final FlagConfigService flagConfigService;

	private final EnvironmentService environmentService;

	private final ApprovalService approvalService;

	private final HistoryService historyService;

	private final RequestContext requestContext;

	public ChangeRequestController(FlagService flagService, FlagConfigService flagConfigService,
			EnvironmentService environmentService, ApprovalService approvalService, HistoryService historyService,
			RequestContext requestContext) {
		this.flagService = flagService;
		this.flagConfigService = flagConfigService;
		this.environmentService = environmentService;
		this.approvalService = approvalService;
		this.historyService = historyService;
		this.requestContext = requestContext;
	}

	@PostMapping("/api/v1/flags/{flagId}/environments/{environmentKey}/changes")
	public Map<String, Object> submitChange(@PathVariable String flagId, @PathVariable String environmentKey,
			@RequestBody(required = false) Map<String, Object> body) {
		FeatureFlag flag = flagService.get(flagId);
		Environment environment = environmentService.requireByKey(flag.getProjectId(), environmentKey);
		Map<String, Object> input = Requests.body(body);
		String reason = Requests.string(input, "reason");

		return submit(flag, environment, input, reason, "CONFIG_UPDATED",
				requestContext.account().getName() + " updated " + environment.getName() + ".");
	}

	@PostMapping("/api/v1/flags/{flagId}/environments/{environmentKey}/toggle")
	public Map<String, Object> toggle(@PathVariable String flagId, @PathVariable String environmentKey,
			@RequestBody(required = false) Map<String, Object> body) {
		FeatureFlag flag = flagService.get(flagId);
		Environment environment = environmentService.requireByKey(flag.getProjectId(), environmentKey);
		Boolean enabled = Requests.bool(Requests.body(body), "enabled");

		if (enabled == null) {
			throw new com.featureflags.shared.ApiException(400, "VALIDATION_ERROR", "enabled is required.");
		}

		FlagEnvironmentConfig current = flagConfigService.getOrCreate(flag, environment);
		Map<String, Object> fullChange = changeShape(current);
		fullChange.put("enabled", enabled);

		String reason = (enabled ? "Turned on " : "Turned off ") + flag.getName() + " in " + environment.getName();

		return submit(flag, environment, fullChange, reason, "CONFIG_UPDATED", reason + ".");
	}

	/**
	 * Restores a past configuration snapshot from history. Routed through the same
	 * production-approval gate as every other change - a rollback in Production is
	 * still a change to what Production serves, so it still needs a second reviewer.
	 */
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

		String reason = "Restore the " + environment.getName() + " configuration from " + entry.getCreatedAt() + ".";
		String directApplySummary = requestContext.account().getName() + " restored the " + environment.getName()
				+ " configuration from " + entry.getCreatedAt() + ".";

		return submit(flag, environment, snapshotChangeShape(entry.getAfterSnapshot()), reason, "ROLLED_BACK",
				directApplySummary);
	}

	/** Strips a history snapshot down to the fields {@link FlagConfigValidator} reads, same as {@link #changeShape}. */
	private Map<String, Object> snapshotChangeShape(Map<String, Object> snapshot) {
		Map<String, Object> shape = new LinkedHashMap<>();
		shape.put("enabled", snapshot.get("enabled"));
		shape.put("offVariationId", snapshot.get("offVariationId"));
		shape.put("targets", snapshot.get("targets"));
		shape.put("rules", snapshot.get("rules"));

		if (snapshot.get("fallthroughVariationId") != null) {
			shape.put("fallthroughVariationId", snapshot.get("fallthroughVariationId"));
		} else {
			shape.put("fallthroughRollout", snapshot.get("fallthroughRollout"));
		}

		return shape;
	}

	/** Extracts only the fields {@link FlagConfigValidator} reads, so proposals don't carry document metadata. */
	private Map<String, Object> changeShape(FlagEnvironmentConfig config) {
		Map<String, Object> shape = new LinkedHashMap<>();
		shape.put("enabled", config.isEnabled());
		shape.put("offVariationId", config.getOffVariationId());
		shape.put("targets", config.toMap().get("targets"));
		shape.put("rules", config.toMap().get("rules"));

		if (config.getFallthroughVariationId() != null) {
			shape.put("fallthroughVariationId", config.getFallthroughVariationId());
		} else {
			shape.put("fallthroughRollout", config.toMap().get("fallthroughRollout"));
		}

		return shape;
	}

	private Map<String, Object> submit(FeatureFlag flag, Environment environment, Map<String, Object> input,
			String reason, String directApplyAction, String directApplySummary) {
		if (environment.isProduction()) {
			FlagConfigValidator.validate(flag, input);
			ApprovalRequest request = approvalService.propose(flag, environment, input, reason, directApplyAction,
					requestContext.account());

			return Map.of("data", Map.of("applied", false, "approvalRequest", request.toMap()));
		}

		FlagConfigValidator.ParsedChange change = FlagConfigValidator.validate(flag, input);
		FlagEnvironmentConfig config = flagConfigService.applyDirect(flag, environment, change,
				requestContext.account(), directApplyAction, directApplySummary);

		return Map.of("data", Map.of("applied", true, "config", config.toMap()));
	}
}

