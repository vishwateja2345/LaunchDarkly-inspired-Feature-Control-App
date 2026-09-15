package com.featureflags.flags;

import java.util.LinkedHashMap;
import java.util.Map;

/** The outcome of evaluating a flag for a user: which variation, its value, and why. */
public record EvaluationResult(String variationId, String value, String reason, String ruleId) {

	public static final String REASON_OFF = "FLAG_OFF";

	public static final String REASON_TARGET_MATCH = "TARGET_MATCH";

	public static final String REASON_RULE_MATCH = "RULE_MATCH";

	public static final String REASON_FALLTHROUGH = "FALLTHROUGH";

	public static final String REASON_ERROR = "ERROR";

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("variationId", variationId);
		body.put("value", value);
		body.put("reason", reason);
		body.put("ruleId", ruleId);

		return body;
	}
}
