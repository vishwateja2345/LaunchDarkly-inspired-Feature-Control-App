package com.featureflags.flags;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.ObjectIds;
import com.featureflags.shared.Requests;

/**
 * Validates a full environment-config replacement: the shape the "edit targeting"
 * screen submits. Every variation reference is checked against the flag's own
 * variation list, and rollouts must sum to (approximately) 100.
 */
public final class FlagConfigValidator {

	private static final double ROLLOUT_SUM_TOLERANCE = 0.5;

	private static final List<String> ATTRIBUTE_OPERATORS = List.of("equals", "notEquals", "in", "notIn", "contains",
			"greaterThan", "lessThan");

	private static final List<String> ALL_OPERATORS = List.of("equals", "notEquals", "in", "notIn", "contains",
			"greaterThan", "lessThan", "segmentMatch");

	public record ParsedChange(boolean enabled, String offVariationId, List<Target> targets, List<Rule> rules,
			String fallthroughVariationId, List<RolloutWeight> fallthroughRollout) {
	}

	public static ParsedChange validate(FeatureFlag flag, Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		Set<String> variationIds = new LinkedHashSet<>();
		flag.getVariations().forEach(variation -> variationIds.add(variation.getId()));

		Boolean enabled = Requests.bool(input, "enabled");

		if (enabled == null) {
			errors.add("enabled", Messages.expected("boolean", "undefined"));
		}

		String offVariationId = readOptionalVariationRef(input, "offVariationId", variationIds, errors);
		List<Target> targets = readTargets(input, variationIds, errors);
		List<Rule> rules = readRules(input, variationIds, errors);

		boolean hasFallthroughVariation = input.containsKey("fallthroughVariationId")
				&& input.get("fallthroughVariationId") != null;
		List<Object> rawFallthroughRollout = Requests.list(input, "fallthroughRollout");
		boolean hasFallthroughRollout = rawFallthroughRollout != null && !rawFallthroughRollout.isEmpty();

		String fallthroughVariationId = null;
		List<RolloutWeight> fallthroughRollout = new ArrayList<>();

		if (hasFallthroughVariation == hasFallthroughRollout) {
			errors.addFormError("Provide exactly one of fallthroughVariationId or fallthroughRollout.");
		} else if (hasFallthroughVariation) {
			fallthroughVariationId = readOptionalVariationRef(input, "fallthroughVariationId", variationIds, errors);
		} else {
			fallthroughRollout = readRollout(rawFallthroughRollout, variationIds, errors, "fallthroughRollout");
		}

		errors.throwIfAny();

		return new ParsedChange(Boolean.TRUE.equals(enabled), offVariationId, targets, rules, fallthroughVariationId,
				fallthroughRollout);
	}

	private static String readOptionalVariationRef(Map<String, Object> input, String field, Set<String> variationIds,
			FieldErrors errors) {
		String value = Requests.string(input, field);

		if (value == null) {
			return null;
		}

		if (!variationIds.contains(value)) {
			errors.add(field, "References a variation that does not exist on this flag.");
		}

		return value;
	}

	@SuppressWarnings("unchecked")
	private static List<Target> readTargets(Map<String, Object> input, Set<String> variationIds, FieldErrors errors) {
		List<Object> raw = Requests.list(input, "targets");
		List<Target> targets = new ArrayList<>();

		if (raw == null) {
			return targets;
		}

		for (int index = 0; index < raw.size(); index += 1) {
			if (!(raw.get(index) instanceof Map<?, ?> rawTarget)) {
				errors.add("targets[" + index + "]", Messages.expected("object", "undefined"));
				continue;
			}

			Map<String, Object> targetInput = (Map<String, Object>) rawTarget;
			String variationId = Requests.string(targetInput, "variationId");
			List<String> userKeys = Requests.stringList(targetInput, "userKeys");

			if (variationId == null || !variationIds.contains(variationId)) {
				errors.add("targets[" + index + "].variationId", "References a variation that does not exist on this flag.");
			}

			Target target = new Target();
			target.setVariationId(variationId);
			target.setUserKeys(userKeys == null ? List.of() : userKeys);
			targets.add(target);
		}

		return targets;
	}

	@SuppressWarnings("unchecked")
	private static List<Rule> readRules(Map<String, Object> input, Set<String> variationIds, FieldErrors errors) {
		List<Object> raw = Requests.list(input, "rules");
		List<Rule> rules = new ArrayList<>();

		if (raw == null) {
			return rules;
		}

		for (int index = 0; index < raw.size(); index += 1) {
			String prefix = "rules[" + index + "]";

			if (!(raw.get(index) instanceof Map<?, ?> rawRule)) {
				errors.add(prefix, Messages.expected("object", "undefined"));
				continue;
			}

			Map<String, Object> ruleInput = (Map<String, Object>) rawRule;
			Rule rule = new Rule();
			String id = Requests.string(ruleInput, "id");
			rule.setId(id == null || id.isBlank() ? ObjectIds.next() : id);
			String description = Requests.string(ruleInput, "description");
			rule.setDescription(description == null ? "" : description.trim());
			rule.setClauses(readClauses(ruleInput, prefix, errors));

			boolean hasVariation = ruleInput.containsKey("variationId") && ruleInput.get("variationId") != null;
			List<Object> rawRollout = Requests.list(ruleInput, "rollout");
			boolean hasRollout = rawRollout != null && !rawRollout.isEmpty();

			if (hasVariation == hasRollout) {
				errors.addFormError(prefix + ": provide exactly one of variationId or rollout.");
			} else if (hasVariation) {
				rule.setVariationId(readOptionalVariationRef(ruleInput, "variationId", variationIds, errors));
			} else {
				rule.setRollout(readRollout(rawRollout, variationIds, errors, prefix + ".rollout"));
			}

			if (rule.getClauses().isEmpty()) {
				errors.add(prefix + ".clauses", Messages.tooSmallArray(1));
			}

			rules.add(rule);
		}

		return rules;
	}

	@SuppressWarnings("unchecked")
	private static List<Clause> readClauses(Map<String, Object> ruleInput, String prefix, FieldErrors errors) {
		List<Object> raw = Requests.list(ruleInput, "clauses");
		List<Clause> clauses = new ArrayList<>();

		if (raw == null) {
			return clauses;
		}

		for (int index = 0; index < raw.size(); index += 1) {
			String clausePrefix = prefix + ".clauses[" + index + "]";

			if (!(raw.get(index) instanceof Map<?, ?> rawClause)) {
				errors.add(clausePrefix, Messages.expected("object", "undefined"));
				continue;
			}

			Map<String, Object> clauseInput = (Map<String, Object>) rawClause;
			String operator = Requests.string(clauseInput, "operator");
			String attribute = Requests.string(clauseInput, "attribute");
			List<String> values = Requests.stringList(clauseInput, "values");

			if (operator == null || !ALL_OPERATORS.contains(operator)) {
				errors.add(clausePrefix + ".operator", Messages.invalidOption(ALL_OPERATORS));
			}

			if (ATTRIBUTE_OPERATORS.contains(operator) && (attribute == null || attribute.isBlank())) {
				errors.add(clausePrefix + ".attribute", Messages.MISSING_STRING);
			}

			if (values == null || values.isEmpty()) {
				errors.add(clausePrefix + ".values", Messages.tooSmallArray(1));
			}

			Clause clause = new Clause();
			clause.setAttribute(attribute);
			clause.setOperator(operator);
			clause.setValues(values == null ? List.of() : values);
			clauses.add(clause);
		}

		return clauses;
	}

	@SuppressWarnings("unchecked")
	private static List<RolloutWeight> readRollout(List<Object> raw, Set<String> variationIds, FieldErrors errors,
			String field) {
		List<RolloutWeight> weights = new ArrayList<>();

		if (raw == null) {
			errors.add(field, Messages.tooSmallArray(1));

			return weights;
		}

		double total = 0;

		for (int index = 0; index < raw.size(); index += 1) {
			if (!(raw.get(index) instanceof Map<?, ?> rawWeight)) {
				errors.add(field + "[" + index + "]", Messages.expected("object", "undefined"));
				continue;
			}

			Map<String, Object> weightInput = (Map<String, Object>) rawWeight;
			String variationId = Requests.string(weightInput, "variationId");
			Double weight = Requests.number(weightInput, "weight");

			if (variationId == null || !variationIds.contains(variationId)) {
				errors.add(field + "[" + index + "].variationId", "References a variation that does not exist on this flag.");
			}

			if (weight == null || weight < 0 || weight > 100) {
				errors.add(field + "[" + index + "].weight", "Invalid input: expected a number between 0 and 100");
			} else {
				total += weight;
			}

			RolloutWeight rolloutWeight = new RolloutWeight();
			rolloutWeight.setVariationId(variationId);
			rolloutWeight.setWeight(weight == null ? 0 : weight);
			weights.add(rolloutWeight);
		}

		if (!weights.isEmpty() && Math.abs(total - 100.0) > ROLLOUT_SUM_TOLERANCE) {
			errors.add(field, "Rollout weights must add up to 100 (currently " + total + ").");
		}

		return weights;
	}

	private FlagConfigValidator() {
	}
}
