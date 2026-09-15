package com.featureflags.flags;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Restoring a config from history lives in {@code ChangeRequestController} instead of
 * here, so it goes through the exact same production-approval gate as every other
 * change rather than a second, easy-to-drift-out-of-sync code path.
 */
@RestController
public class FlagConfigController {

	private final FlagService flagService;

	private final FlagConfigService flagConfigService;

	public FlagConfigController(FlagService flagService, FlagConfigService flagConfigService) {
		this.flagService = flagService;
		this.flagConfigService = flagConfigService;
	}

	@GetMapping("/api/v1/flags/{flagId}/environments/{environmentKey}/config")
	public Map<String, Object> getConfig(@PathVariable String flagId, @PathVariable String environmentKey) {
		FeatureFlag flag = flagService.get(flagId);

		return Map.of("data", flagConfigService.getByEnvironmentKey(flag, environmentKey).toMap());
	}
}
