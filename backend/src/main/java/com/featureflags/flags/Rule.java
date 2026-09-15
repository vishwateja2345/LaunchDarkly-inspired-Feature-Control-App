package com.featureflags.flags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An ordered targeting rule. All clauses must match (AND) for the rule to apply. A
 * matching rule resolves to either a fixed variation or a percentage rollout among
 * variations - exactly one of the two should be set.
 */
public class Rule {

	private String id;

	private String description = "";

	private List<Clause> clauses = new ArrayList<>();

	private String variationId;

	private List<RolloutWeight> rollout = new ArrayList<>();

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", id);
		body.put("description", description);
		body.put("clauses", clauses.stream().map(Clause::toMap).toList());
		body.put("variationId", variationId);
		body.put("rollout", rollout.stream().map(RolloutWeight::toMap).toList());

		return body;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Clause> getClauses() {
		return clauses;
	}

	public void setClauses(List<Clause> clauses) {
		this.clauses = clauses;
	}

	public String getVariationId() {
		return variationId;
	}

	public void setVariationId(String variationId) {
		this.variationId = variationId;
	}

	public List<RolloutWeight> getRollout() {
		return rollout;
	}

	public void setRollout(List<RolloutWeight> rollout) {
		this.rollout = rollout;
	}
}
