package com.featureflags.history;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.featureflags.shared.Dates;

@Document(collection = "audit_log_entries")
public class AuditLogEntry {

	@Id
	private String id;

	@Field(targetType = FieldType.OBJECT_ID)
	private String projectId;

	@Field(targetType = FieldType.OBJECT_ID)
	private String flagId;

	@Field(targetType = FieldType.OBJECT_ID)
	private String environmentId;

	private String environmentKey;

	private String action;

	private String summary;

	private Map<String, Object> beforeSnapshot;

	private Map<String, Object> afterSnapshot;

	@Field(targetType = FieldType.OBJECT_ID)
	private String actorId;

	private String actorName;

	private Instant createdAt = Instant.now();

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("projectId", projectId);
		body.put("flagId", flagId);
		body.put("environmentId", environmentId);
		body.put("environmentKey", environmentKey);
		body.put("action", action);
		body.put("summary", summary);
		body.put("beforeSnapshot", beforeSnapshot);
		body.put("afterSnapshot", afterSnapshot);
		body.put("actorId", actorId);
		body.put("actorName", actorName);
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

	public String getFlagId() {
		return flagId;
	}

	public void setFlagId(String flagId) {
		this.flagId = flagId;
	}

	public String getEnvironmentId() {
		return environmentId;
	}

	public void setEnvironmentId(String environmentId) {
		this.environmentId = environmentId;
	}

	public String getEnvironmentKey() {
		return environmentKey;
	}

	public void setEnvironmentKey(String environmentKey) {
		this.environmentKey = environmentKey;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Map<String, Object> getBeforeSnapshot() {
		return beforeSnapshot;
	}

	public void setBeforeSnapshot(Map<String, Object> beforeSnapshot) {
		this.beforeSnapshot = beforeSnapshot;
	}

	public Map<String, Object> getAfterSnapshot() {
		return afterSnapshot;
	}

	public void setAfterSnapshot(Map<String, Object> afterSnapshot) {
		this.afterSnapshot = afterSnapshot;
	}

	public String getActorId() {
		return actorId;
	}

	public void setActorId(String actorId) {
		this.actorId = actorId;
	}

	public String getActorName() {
		return actorName;
	}

	public void setActorName(String actorName) {
		this.actorName = actorName;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
