package com.featureflags.experiments;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.flags.FeatureFlag;
import com.featureflags.flags.FlagService;
import com.featureflags.flags.Variation;

@Service
public class ExperimentService {

	private static final double Z_95 = 1.96;

	private final ExperimentRepository experimentRepository;

	private final FlagService flagService;

	private final EnvironmentService environmentService;

	public ExperimentService(ExperimentRepository experimentRepository, FlagService flagService,
			EnvironmentService environmentService) {
		this.experimentRepository = experimentRepository;
		this.flagService = flagService;
		this.environmentService = environmentService;
	}

	public ExperimentEvent recordExposure(String flagId, String environmentKey, ExperimentValidator.ExposureInput input) {
		FeatureFlag flag = flagService.get(flagId);
		Environment environment = environmentService.requireByKey(flag.getProjectId(), environmentKey);

		ExperimentEvent event = new ExperimentEvent();
		event.setFlagId(flagId);
		event.setEnvironmentId(environment.getId());
		event.setEnvironmentKey(environment.getKey());
		event.setUserKey(input.userKey());
		event.setVariationId(input.variationId());
		event.setEventType(ExperimentEvent.TYPE_EXPOSURE);

		return experimentRepository.insert(event);
	}

	public ExperimentEvent recordConversion(String flagId, String environmentKey,
			ExperimentValidator.ConversionInput input) {
		FeatureFlag flag = flagService.get(flagId);
		Environment environment = environmentService.requireByKey(flag.getProjectId(), environmentKey);

		ExperimentEvent event = new ExperimentEvent();
		event.setFlagId(flagId);
		event.setEnvironmentId(environment.getId());
		event.setEnvironmentKey(environment.getKey());
		event.setUserKey(input.userKey());
		event.setMetricKey(input.metricKey());
		event.setValue(input.value());
		event.setEventType(ExperimentEvent.TYPE_CONVERSION);

		return experimentRepository.insert(event);
	}

	public Set<String> metricKeys(String flagId, String environmentKey) {
		FeatureFlag flag = flagService.get(flagId);
		Environment environment = environmentService.requireByKey(flag.getProjectId(), environmentKey);

		return experimentRepository.distinctMetricKeys(flagId, environment.getId());
	}

	public Map<String, Object> compare(String flagId, String environmentKey, String metricKey) {
		FeatureFlag flag = flagService.get(flagId);
		Environment environment = environmentService.requireByKey(flag.getProjectId(), environmentKey);

		List<ExperimentEvent> exposures = experimentRepository.findByFlagAndEnvironment(flagId, environment.getId(),
				ExperimentEvent.TYPE_EXPOSURE, null);
		List<ExperimentEvent> conversions = metricKey == null ? List.of()
				: experimentRepository.findByFlagAndEnvironment(flagId, environment.getId(),
						ExperimentEvent.TYPE_CONVERSION, metricKey);

		Map<String, VariationStats> stats = new LinkedHashMap<>();

		for (Variation variation : flag.getVariations()) {
			stats.put(variation.getId(), new VariationStats());
		}

		// Conversion events don't carry their own variationId (a real SDK reports "user X
		// converted on metric Z" independently of exposure) - attribute each conversion back to
		// the variation the same user was first exposed to, joining the two event streams by key.
		Map<String, String> variationByUser = new LinkedHashMap<>();

		for (ExperimentEvent event : exposures) {
			stats.computeIfAbsent(event.getVariationId(), key -> new VariationStats()).exposedUsers
					.add(event.getUserKey());
			variationByUser.putIfAbsent(event.getUserKey(), event.getVariationId());
		}

		for (ExperimentEvent event : conversions) {
			String variationId = variationByUser.get(event.getUserKey());

			if (variationId == null) {
				continue;
			}

			VariationStats variationStats = stats.computeIfAbsent(variationId, key -> new VariationStats());

			if (variationStats.convertedUsers.add(event.getUserKey()) && event.getValue() != null) {
				variationStats.totalValue += event.getValue();
			}
		}

		String controlVariationId = flag.getVariations().isEmpty() ? null : flag.getVariations().get(0).getId();
		VariationStats control = stats.get(controlVariationId);

		List<Map<String, Object>> rows = new ArrayList<>();

		for (Variation variation : flag.getVariations()) {
			VariationStats variationStats = stats.getOrDefault(variation.getId(), new VariationStats());
			boolean isControl = variation.getId().equals(controlVariationId);
			rows.add(toRow(variation, variationStats, isControl ? null : control));
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("flagId", flagId);
		body.put("environmentKey", environmentKey);
		body.put("metricKey", metricKey);
		body.put("controlVariationId", controlVariationId);
		body.put("variations", rows);

		return body;
	}

	private Map<String, Object> toRow(Variation variation, VariationStats variationStats, VariationStats control) {
		int exposures = variationStats.exposedUsers.size();
		int conversions = variationStats.convertedUsers.size();
		double rate = exposures == 0 ? 0 : (double) conversions / exposures;

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("variationId", variation.getId());
		row.put("variationName", variation.getName());
		row.put("exposures", exposures);
		row.put("conversions", conversions);
		row.put("conversionRate", round(rate * 100));
		row.put("totalValue", round(variationStats.totalValue));
		row.put("averageValue", round(conversions == 0 ? 0 : variationStats.totalValue / conversions));

		if (control != null) {
			double controlRate = control.exposedUsers.isEmpty() ? 0
					: (double) control.convertedUsers.size() / control.exposedUsers.size();
			double relativeLift = controlRate == 0 ? 0 : ((rate - controlRate) / controlRate) * 100;
			row.put("relativeLiftPercent", round(relativeLift));
			row.put("isSignificant", isSignificant(control, variationStats));
		} else {
			row.put("relativeLiftPercent", null);
			row.put("isSignificant", null);
		}

		return row;
	}

	/** Two-proportion z-test at ~95% confidence, the standard lightweight A/B significance check. */
	private boolean isSignificant(VariationStats control, VariationStats variant) {
		int exposedControl = control.exposedUsers.size();
		int exposedVariant = variant.exposedUsers.size();

		if (exposedControl == 0 || exposedVariant == 0) {
			return false;
		}

		double p1 = (double) control.convertedUsers.size() / exposedControl;
		double p2 = (double) variant.convertedUsers.size() / exposedVariant;
		double pooled = (double) (control.convertedUsers.size() + variant.convertedUsers.size())
				/ (exposedControl + exposedVariant);
		double standardError = Math.sqrt(pooled * (1 - pooled) * (1.0 / exposedControl + 1.0 / exposedVariant));

		if (standardError == 0) {
			return false;
		}

		double z = (p2 - p1) / standardError;

		return Math.abs(z) >= Z_95;
	}

	private double round(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	private static final class VariationStats {

		private final Set<String> exposedUsers = new java.util.LinkedHashSet<>();

		private final Set<String> convertedUsers = new java.util.LinkedHashSet<>();

		private double totalValue;
	}
}
