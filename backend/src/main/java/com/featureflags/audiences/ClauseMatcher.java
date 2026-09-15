package com.featureflags.audiences;

import java.util.List;

import com.featureflags.shared.EvaluationContext;

/**
 * Shared attribute-comparison logic used by both segment rules and flag targeting
 * rule clauses, so the two rule languages stay consistent.
 */
public final class ClauseMatcher {

	public static boolean matches(String attribute, String operator, List<String> values, EvaluationContext context) {
		Object actual = context.attribute(attribute);
		String text = actual == null ? null : String.valueOf(actual);

		return switch (operator == null ? "in" : operator) {
			case "equals" -> text != null && values.size() == 1 && text.equalsIgnoreCase(values.get(0));
			case "notEquals" -> text == null || values.stream().noneMatch(text::equalsIgnoreCase);
			case "in" -> text != null && values.stream().anyMatch(text::equalsIgnoreCase);
			case "notIn" -> text == null || values.stream().noneMatch(text::equalsIgnoreCase);
			case "contains" -> text != null && values.stream().anyMatch(value -> text.toLowerCase().contains(value.toLowerCase()));
			case "greaterThan" -> compareNumeric(text, values, 1);
			case "lessThan" -> compareNumeric(text, values, -1);
			default -> false;
		};
	}

	private static boolean compareNumeric(String text, List<String> values, int expectedSign) {
		if (text == null || values.isEmpty()) {
			return false;
		}

		try {
			double actual = Double.parseDouble(text);
			double bound = Double.parseDouble(values.get(0));

			return Double.compare(actual, bound) == expectedSign;
		} catch (NumberFormatException exception) {
			return false;
		}
	}

	private ClauseMatcher() {
	}
}
