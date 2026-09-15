package com.featureflags.audiences;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "segments")
public class Segment {

	@Id
	private String id;

	@Field(targetType = FieldType.OBJECT_ID)
	private String projectId;

	private String key;

	private String name;

	private String description = "";

	/** All rules must match (AND) for a user to be included via rules. */
	private List<SegmentRule> rules = new ArrayList<>();

	private List<String> includedKeys = new ArrayList<>();

	private List<String> excludedKeys = new ArrayList<>();

	@CreatedDate
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("projectId", projectId);
		body.put("key", key);
		body.put("name", name);
		body.put("description", description);
		body.put("rules", rules.stream().map(rule -> {
			Map<String, Object> ruleMap = new LinkedHashMap<>();
			ruleMap.put("attribute", rule.getAttribute());
			ruleMap.put("operator", rule.getOperator());
			ruleMap.put("values", rule.getValues());
			return ruleMap;
		}).toList());
		body.put("includedKeys", includedKeys);
		body.put("excludedKeys", excludedKeys);
		body.put("createdAt", com.featureflags.shared.Dates.iso(createdAt));
		body.put("updatedAt", com.featureflags.shared.Dates.iso(updatedAt));

		return body;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
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

	public List<SegmentRule> getRules() {
		return rules;
	}

	public void setRules(List<SegmentRule> rules) {
		this.rules = rules;
	}

	public List<String> getIncludedKeys() {
		return includedKeys;
	}

	public void setIncludedKeys(List<String> includedKeys) {
		this.includedKeys = includedKeys;
	}

	public List<String> getExcludedKeys() {
		return excludedKeys;
	}

	public void setExcludedKeys(List<String> excludedKeys) {
		this.excludedKeys = excludedKeys;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
