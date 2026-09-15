package com.featureflags.flags;

import java.util.LinkedHashMap;
import java.util.Map;

/** One possible value a flag can resolve to. Boolean flags have exactly two: true/false. */
public class Variation {

	private String id;

	private String value;

	private String name;

	private String description = "";

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", id);
		body.put("value", value);
		body.put("name", name);
		body.put("description", description);

		return body;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
