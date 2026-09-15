package com.featureflags.experiments;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.featureflags.shared.Dates;

@Document(collection = "experiment_events")
public class ExperimentEvent {

	public static final String TYPE_EXPOSURE = "exposure";

	public static final String TYPE_CONVERSION = "conversion";

	@Id
	private String id;

	@Field(targetType = FieldType.OBJECT_ID)
	private String flagId;

	@Field(targetType = FieldType.OBJECT_ID)
	private String environmentId;

	private String environmentKey;

	private String variationId;

	private String userKey;

	private String eventType;

	private String metricKey;

	private Double value;

	private Instant createdAt = Instant.now();

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("flagId", flagId);
		body.put("environmentId", environmentId);
		body.put("environmentKey", environmentKey);
		body.put("variationId", variationId);
		body.put("userKey", userKey);
		body.put("eventType", eventType);
		body.put("metricKey", metricKey);
		body.put("value", value);
		body.put("createdAt", Dates.iso(createdAt));

		return body;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getVariationId() {
		return variationId;
	}

	public void setVariationId(String variationId) {
		this.variationId = variationId;
	}

	public String getUserKey() {
		return userKey;
	}

	public void setUserKey(String userKey) {
		this.userKey = userKey;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getMetricKey() {
		return metricKey;
	}

	public void setMetricKey(String metricKey) {
		this.metricKey = metricKey;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
