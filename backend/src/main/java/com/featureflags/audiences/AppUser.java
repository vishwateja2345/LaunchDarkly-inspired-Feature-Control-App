package com.featureflags.audiences;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.featureflags.shared.Dates;
import com.featureflags.shared.EvaluationContext;

@Document(collection = "app_users")
public class AppUser {

	@Id
	private String id;

	@Field(targetType = FieldType.OBJECT_ID)
	private String projectId;

	private String key;

	private String name;

	private String email;

	private String plan = "free";

	private String country = "US";

	private Map<String, Object> attributes = new LinkedHashMap<>();

	@CreatedDate
	private Instant createdAt;

	public EvaluationContext toContext() {
		return new EvaluationContext(key, name, email, plan, country, attributes);
	}

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("projectId", projectId);
		body.put("key", key);
		body.put("name", name);
		body.put("email", email);
		body.put("plan", plan);
		body.put("country", country);
		body.put("attributes", attributes);
		body.put("createdAt", Dates.iso(createdAt));

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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPlan() {
		return plan;
	}

	public void setPlan(String plan) {
		this.plan = plan;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
