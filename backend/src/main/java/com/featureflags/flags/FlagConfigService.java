package com.featureflags.flags;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.auth.Account;
import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.history.HistoryService;
import com.featureflags.shared.EvaluationContext;

@Service
public class FlagConfigService {

	private final FlagConfigRepository flagConfigRepository;

	private final EnvironmentService environmentService;

	private final FlagEvaluator flagEvaluator;

	private final HistoryService historyService;

	public FlagConfigService(FlagConfigRepository flagConfigRepository, EnvironmentService environmentService,
			FlagEvaluator flagEvaluator, HistoryService historyService) {
		this.flagConfigRepository = flagConfigRepository;
		this.environmentService = environmentService;
		this.flagEvaluator = flagEvaluator;
		this.historyService = historyService;
	}

	public FlagEnvironmentConfig getOrCreate(FeatureFlag flag, Environment environment) {
		FlagEnvironmentConfig config = flagConfigRepository.find(flag.getId(), environment.getId());

		if (config != null) {
			return config;
		}

		FlagEnvironmentConfig created = new FlagEnvironmentConfig();
		created.setFlagId(flag.getId());
		created.setEnvironmentId(environment.getId());
		created.setEnvironmentKey(environment.getKey());
		created.setEnabled(false);
		String defaultVariationId = flag.getVariations().isEmpty() ? null : flag.getVariations().get(0).getId();
		created.setOffVariationId(defaultVariationId);
		created.setFallthroughVariationId(defaultVariationId);

		return flagConfigRepository.save(created);
	}

	public FlagEnvironmentConfig getByEnvironmentKey(FeatureFlag flag, String environmentKey) {
		Environment environment = environmentService.requireByKey(flag.getProjectId(), environmentKey);

		return getOrCreate(flag, environment);
	}

	/** Applies a validated change to a config immediately and records the resulting history entry. */
	public FlagEnvironmentConfig applyDirect(FeatureFlag flag, Environment environment,
			FlagConfigValidator.ParsedChange change, Account actor, String action, String summary) {
		FlagEnvironmentConfig config = getOrCreate(flag, environment);
		Map<String, Object> before = config.toMap();

		config.setEnabled(change.enabled());
		config.setOffVariationId(change.offVariationId());
		config.setTargets(change.targets());
		config.setRules(change.rules());
		config.setFallthroughVariationId(change.fallthroughVariationId());
		config.setFallthroughRollout(change.fallthroughRollout());
		config.setVersion(config.getVersion() + 1);
		config.setUpdatedBy(actor == null ? null : actor.getId());
		config.setUpdatedAt(Instant.now());

		FlagEnvironmentConfig saved = flagConfigRepository.save(config);

		historyService.record(flag.getProjectId(), flag.getId(), environment.getId(), environment.getKey(), action,
				summary, before, saved.toMap(), actor);

		return saved;
	}

	public EvaluationResult evaluate(FeatureFlag flag, FlagEnvironmentConfig config, EvaluationContext context) {
		return flagEvaluator.evaluate(flag, config, context);
	}

	public Map<String, Object> evaluateToMap(FeatureFlag flag, FlagEnvironmentConfig config,
			EvaluationContext context) {
		EvaluationResult result = evaluate(flag, config, context);
		Map<String, Object> body = new LinkedHashMap<>(result.toMap());
		body.put("flagKey", flag.getKey());
		body.put("environmentKey", config.getEnvironmentKey());
		body.put("userKey", context.getKey());

		return body;
	}
}
