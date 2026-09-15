package com.featureflags.flags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One condition inside a {@link Rule}. When {@code operator} is {@code segmentMatch},
 * {@code attribute} is ignored and {@code values} holds segment keys (a match on any
 * one of them satisfies the clause).
 */
public class Clause {

	private String attribute;

	private String operator = "in";

	private List<String> values = new ArrayList<>();

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("attribute", attribute);
		body.put("operator", operator);
		body.put("values", values);

		return body;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}
}
