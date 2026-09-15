package com.featureflags.audiences;

import com.featureflags.shared.EvaluationContext;

/** Evaluates whether a user context is a member of a segment. */
public final class SegmentMatcher {

	public static boolean matches(Segment segment, EvaluationContext context) {
		if (segment.getExcludedKeys().contains(context.getKey())) {
			return false;
		}

		if (segment.getIncludedKeys().contains(context.getKey())) {
			return true;
		}

		if (segment.getRules().isEmpty()) {
			return false;
		}

		for (SegmentRule rule : segment.getRules()) {
			if (!ClauseMatcher.matches(rule.getAttribute(), rule.getOperator(), rule.getValues(), context)) {
				return false;
			}
		}

		return true;
	}

	private SegmentMatcher() {
	}
}
