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
import com.featureflags.shared.RequestContext;
import com.featureflags.shared.Requests;

/**
 * The single gateway for changing what a flag serves in an environment. Production
 * environments always require a second person's approval; every other environment
 * applies immediately. Both the quick on/off toggle and the full targeting editor
 * submit through here so there is exactly one code path enforcing that rule.
 */
@RestController
public class ChangeRequestController {

	private final FlagService flagService;

	private final FlagConfigService flagConfigService;

	private final EnvironmentService environmentService;

	private final ApprovalService approvalService;

	private final RequestContext requestContext;

	public ChangeRequestController(FlagService flagService, FlagConfigService flagConfigService,
			EnvironmentService environmentService, ApprovalService approvalService, RequestContext requestContext) {
		this.flagService = flagService;
		this.flagConfigService = flagConfigService;
		this.environmentService = environmentService;
		this.approvalService = approvalService;
		this.requestContext = requestContext;
	}

	@PostMapping("/api/v1/flags/{flagId}/environments/{environmentKey}/changes")
	public Map<String, Object> submitChange(@PathVariable String flagId, @PathVariable String environmentKey,
			@RequestBody(required = false) Map<String, Object> body) {
		FeatureFlag flag = flagService.get(flagId);
		Environment environment = environmentService.requireByKey(flag.getProjectId(), environmentKey);
		Map<String, Object> input = Requests.body(body);
		String reason = Requests.string(input, "reason");

		return submit(flag, environment, input, reason);
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

		return submit(flag, environment, fullChange, reason);
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
			String reason) {
		if (environment.isProduction()) {
			FlagConfigValidator.validate(flag, input);
			ApprovalRequest request = approvalService.propose(flag, environment, input, reason,
					requestContext.account());

			return Map.of("data", Map.of("applied", false, "approvalRequest", request.toMap()));
		}

		FlagConfigValidator.ParsedChange change = FlagConfigValidator.validate(flag, input);
		FlagEnvironmentConfig config = flagConfigService.applyDirect(flag, environment, change,
				requestContext.account(), "CONFIG_UPDATED",
				requestContext.account().getName() + " updated " + environment.getName() + ".");

		return Map.of("data", Map.of("applied", true, "config", config.toMap()));
	}
}
