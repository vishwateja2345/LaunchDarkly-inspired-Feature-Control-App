package com.featureflags.audiences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class SegmentValidator {

	private static final List<String> OPERATORS = List.of("equals", "notEquals", "in", "notIn", "contains",
			"greaterThan", "lessThan");

	private static final int NAME_MAX = 80;

	public record Input(String name, String key, String description, List<SegmentRule> rules,
			List<String> includedKeys, List<String> excludedKeys) {
	}

	public static Input validate(Map<String, Object> body, boolean requireKey) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String name = readName(input, errors);
		String key = requireKey ? readKey(input, errors, name) : Requests.string(input, "key");
		String description = Requests.string(input, "description");
		List<SegmentRule> rules = readRules(input, errors);
		List<String> includedKeys = Requests.stringList(input, "includedKeys");
		List<String> excludedKeys = Requests.stringList(input, "excludedKeys");

		errors.throwIfAny();

		return new Input(name, key, description == null ? "" : description.trim(), rules,
				includedKeys == null ? List.of() : includedKeys, excludedKeys == null ? List.of() : excludedKeys);
	}

	private static String readName(Map<String, Object> input, FieldErrors errors) {
		String name = Requests.string(input, "name");

		if (name == null || name.trim().isEmpty()) {
			errors.add("name", Messages.MISSING_STRING);

			return null;
		}

		String trimmed = name.trim();

		if (trimmed.length() > NAME_MAX) {
			errors.add("name", Requests.tooBigString(NAME_MAX));
		}

		return trimmed;
	}

	private static String readKey(Map<String, Object> input, FieldErrors errors, String fallbackName) {
		String key = Requests.string(input, "key");
		String candidate = key != null ? key.trim()
				: com.featureflags.projects.ProjectKeys.slugify(fallbackName == null ? "" : fallbackName);

		if (candidate.isEmpty()) {
			errors.add("key", Messages.MISSING_STRING);
		}

		return candidate;
	}

	@SuppressWarnings("unchecked")
	private static List<SegmentRule> readRules(Map<String, Object> input, FieldErrors errors) {
		List<Object> raw = Requests.list(input, "rules");

		if (raw == null) {
			return new ArrayList<>();
		}

		List<SegmentRule> rules = new ArrayList<>();

		for (int index = 0; index < raw.size(); index += 1) {
			if (!(raw.get(index) instanceof Map<?, ?> rawRule)) {
				errors.add("rules[" + index + "]", Messages.expected("object", "undefined"));
				continue;
			}

			Map<String, Object> ruleInput = (Map<String, Object>) rawRule;
			String attribute = Requests.string(ruleInput, "attribute");
			String operator = Requests.string(ruleInput, "operator");
			List<String> values = Requests.stringList(ruleInput, "values");

			if (attribute == null || attribute.isBlank()) {
				errors.add("rules[" + index + "].attribute", Messages.MISSING_STRING);
			}

			if (operator == null || !OPERATORS.contains(operator)) {
				errors.add("rules[" + index + "].operator", Messages.invalidOption(OPERATORS));
			}

			if (values == null || values.isEmpty()) {
				errors.add("rules[" + index + "].values", Messages.tooSmallArray(1));
			}

			SegmentRule rule = new SegmentRule();
			rule.setAttribute(attribute);
			rule.setOperator(operator);
			rule.setValues(values == null ? List.of() : values);
			rules.add(rule);
		}

		return rules;
	}

	private SegmentValidator() {
	}
}
