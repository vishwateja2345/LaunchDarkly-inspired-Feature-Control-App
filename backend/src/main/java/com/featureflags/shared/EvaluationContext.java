package com.featureflags.shared;

import java.util.Map;

/**
 * The audience being evaluated against a flag: a stable key plus a handful of
 * well-known attributes and free-form custom attributes. Used by rule clauses,
 * segment matching, and rollout bucketing.
 */
public class EvaluationContext {

	private final String key;

	private final String name;

	private final String email;

	private final String plan;

	private final String country;

	private final Map<String, Object> attributes;

	public EvaluationContext(String key, String name, String email, String plan, String country,
			Map<String, Object> attributes) {
		this.key = key;
		this.name = name;
		this.email = email;
		this.plan = plan;
		this.country = country;
		this.attributes = attributes == null ? Map.of() : attributes;
	}

	public String getKey() {
		return key;
	}

	public Object attribute(String attributeName) {
		return switch (attributeName == null ? "" : attributeName) {
			case "key" -> key;
			case "name" -> name;
			case "email" -> email;
			case "plan" -> plan;
			case "country" -> country;
			default -> attributes.get(attributeName);
		};
	}
}
