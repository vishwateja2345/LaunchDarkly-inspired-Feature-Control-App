package com.featureflags.audiences;

import java.util.ArrayList;
import java.util.List;

/** A single condition inside a {@link Segment}'s rule list, e.g. "plan in [pro, enterprise]". */
public class SegmentRule {

	private String attribute;

	private String operator = "in";

	private List<String> values = new ArrayList<>();

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
