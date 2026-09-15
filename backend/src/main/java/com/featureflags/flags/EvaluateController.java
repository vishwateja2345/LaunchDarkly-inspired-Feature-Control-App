package com.featureflags.flags;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.projects.ProjectService;
import com.featureflags.shared.EvaluationContext;
import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

/** The "SDK-style" evaluation endpoint used by the Try-It panel and by simulated traffic. */
@RestController
public class EvaluateController {

	private final ProjectService projectService;

	private final EnvironmentService environmentService;

	private final FlagService flagService;

	private final FlagConfigService flagConfigService;

	public EvaluateController(ProjectService projectService, EnvironmentService environmentService,
			FlagService flagService, FlagConfigService flagConfigService) {
		this.projectService = projectService;
		this.environmentService = environmentService;
		this.flagService = flagService;
		this.flagConfigService = flagConfigService;
	}

	@PostMapping("/api/v1/evaluate")
	public Map<String, Object> evaluate(@RequestBody(required = false) Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String projectId = Requests.string(input, "projectId");
		String flagKey = Requests.string(input, "flagKey");
		String environmentKey = Requests.string(input, "environmentKey");

		if (projectId == null) {
			errors.add("projectId", Messages.MISSING_STRING);
		}

		if (flagKey == null) {
			errors.add("flagKey", Messages.MISSING_STRING);
		}

		if (environmentKey == null) {
			errors.add("environmentKey", Messages.MISSING_STRING);
		}

		Map<String, Object> userInput = Requests.map(input, "user");

		if (userInput == null || Requests.string(userInput, "key") == null) {
			errors.add("user.key", Messages.MISSING_STRING);
		}

		errors.throwIfAny();

		projectService.requireProject(projectId);
		FeatureFlag flag = flagService.requireByKey(projectId, flagKey);
		Environment environment = environmentService.requireByKey(projectId, environmentKey);
		EvaluationContext context = toContext(userInput);
		FlagEnvironmentConfig config = flagConfigService.getOrCreate(flag, environment);

		return Map.of("data", flagConfigService.evaluateToMap(flag, config, context));
	}

	@SuppressWarnings("unchecked")
	private EvaluationContext toContext(Map<String, Object> userInput) {
		String key = Requests.string(userInput, "key");
		String name = Requests.string(userInput, "name");
		String email = Requests.string(userInput, "email");
		String plan = Requests.string(userInput, "plan");
		String country = Requests.string(userInput, "country");
		Map<String, Object> attributes = Requests.map(userInput, "attributes");

		return new EvaluationContext(key, name, email, plan, country, attributes);
	}
}
