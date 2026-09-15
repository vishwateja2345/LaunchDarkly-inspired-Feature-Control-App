package com.featureflags.flags;

import org.springframework.stereotype.Component;

import com.featureflags.audiences.Segment;
import com.featureflags.audiences.SegmentMatcher;
import com.featureflags.audiences.SegmentRepository;
import com.featureflags.shared.EvaluationContext;

/**
 * The rule-evaluation engine. Resolution order mirrors production feature-flag
 * systems: the flag must be enabled, then individual targets win, then ordered rules
 * (first match wins), then the fallthrough default. Rollouts are resolved with a
 * stable per-user hash so the same user always lands on the same variation.
 */
@Component
public class FlagEvaluator {

	private final SegmentRepository segmentRepository;

	public FlagEvaluator(SegmentRepository segmentRepository) {
		this.segmentRepository = segmentRepository;
	}

	public EvaluationResult evaluate(FeatureFlag flag, FlagEnvironmentConfig config, EvaluationContext context) {
		if (!config.isEnabled()) {
			return resolveFixed(flag, config.getOffVariationId(), EvaluationResult.REASON_OFF, null);
		}

		for (Target target : config.getTargets()) {
			if (target.getUserKeys().contains(context.getKey())) {
				return resolveFixed(flag, target.getVariationId(), EvaluationResult.REASON_TARGET_MATCH, null);
			}
		}

		for (Rule rule : config.getRules()) {
			if (matchesRule(rule, flag.getProjectId(), context)) {
				return resolveRule(flag, rule, context);
			}
		}

		return resolveFallthrough(flag, config, context);
	}

	private boolean matchesRule(Rule rule, String projectId, EvaluationContext context) {
		for (Clause clause : rule.getClauses()) {
			if (!matchesClause(clause, projectId, context)) {
				return false;
			}
		}

		return !rule.getClauses().isEmpty();
	}

	private boolean matchesClause(Clause clause, String projectId, EvaluationContext context) {
		if ("segmentMatch".equals(clause.getOperator())) {
			return clause.getValues().stream()
					.anyMatch(segmentKey -> matchesSegment(projectId, segmentKey, context));
		}

		return com.featureflags.audiences.ClauseMatcher.matches(clause.getAttribute(), clause.getOperator(),
				clause.getValues(), context);
	}

	private boolean matchesSegment(String projectId, String segmentKey, EvaluationContext context) {
		if (projectId == null) {
			return false;
		}

		Segment segment = segmentRepository.findByKey(projectId, segmentKey);

		return segment != null && SegmentMatcher.matches(segment, context);
	}

	private EvaluationResult resolveRule(FeatureFlag flag, Rule rule, EvaluationContext context) {
		if (rule.getVariationId() != null) {
			return resolveFixed(flag, rule.getVariationId(), EvaluationResult.REASON_RULE_MATCH, rule.getId());
		}

		String hashKey = flag.getKey() + "." + context.getKey() + "." + rule.getId();
		String variationId = Bucketing.pickVariation(hashKey, rule.getRollout());

		return resolveFixed(flag, variationId, EvaluationResult.REASON_RULE_MATCH, rule.getId());
	}

	private EvaluationResult resolveFallthrough(FeatureFlag flag, FlagEnvironmentConfig config,
			EvaluationContext context) {
		if (config.getFallthroughVariationId() != null) {
			return resolveFixed(flag, config.getFallthroughVariationId(), EvaluationResult.REASON_FALLTHROUGH, null);
		}

		String hashKey = flag.getKey() + "." + context.getKey() + ".fallthrough";
		String variationId = Bucketing.pickVariation(hashKey, config.getFallthroughRollout());

		return resolveFixed(flag, variationId, EvaluationResult.REASON_FALLTHROUGH, null);
	}

	private EvaluationResult resolveFixed(FeatureFlag flag, String variationId, String reason, String ruleId) {
		if (variationId == null) {
			return new EvaluationResult(null, null, EvaluationResult.REASON_ERROR, ruleId);
		}

		Variation variation = flag.variation(variationId);

		return new EvaluationResult(variationId, variation == null ? null : variation.getValue(), reason, ruleId);
	}
}
