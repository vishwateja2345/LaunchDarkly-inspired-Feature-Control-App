package com.featureflags.flags;

import java.util.LinkedHashMap;
import java.util.Map;

/** One slice of a percentage rollout. Weights across a rollout must sum to 100. */
public class RolloutWeight {

	private String variationId;

	private double weight;

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("variationId", variationId);
		body.put("weight", weight);

		return body;
	}

	public String getVariationId() {
		return variationId;
	}

	public void setVariationId(String variationId) {
		this.variationId = variationId;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
}
