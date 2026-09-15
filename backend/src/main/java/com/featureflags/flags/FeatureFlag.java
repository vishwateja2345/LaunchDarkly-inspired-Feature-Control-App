package com.featureflags.flags;

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

import com.featureflags.shared.Dates;

@Document(collection = "feature_flags")
public class FeatureFlag {

	public static final String TYPE_BOOLEAN = "boolean";

	public static final String TYPE_MULTIVARIATE = "multivariate";

	@Id
	private String id;

	@Field(targetType = FieldType.OBJECT_ID)
	private String projectId;

	private String key;

	private String name;

	private String description = "";

	private String flagType = TYPE_BOOLEAN;

	private List<Variation> variations = new ArrayList<>();

	private List<String> tags = new ArrayList<>();

	private boolean temporary = true;

	private boolean archived;

	private Instant archivedAt;

	@Field(targetType = FieldType.OBJECT_ID)
	private String createdBy;

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
		body.put("flagType", flagType);
		body.put("variations", variations.stream().map(Variation::toMap).toList());
		body.put("tags", tags);
		body.put("temporary", temporary);
		body.put("archived", archived);
		body.put("archivedAt", Dates.iso(archivedAt));
		body.put("createdBy", createdBy);
		body.put("createdAt", Dates.iso(createdAt));
		body.put("updatedAt", Dates.iso(updatedAt));

		return body;
	}

	public Variation variation(String variationId) {
		return variations.stream().filter(variation -> variation.getId().equals(variationId)).findFirst()
				.orElse(null);
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

	public String getFlagType() {
		return flagType;
	}

	public void setFlagType(String flagType) {
		this.flagType = flagType;
	}

	public List<Variation> getVariations() {
		return variations;
	}

	public void setVariations(List<Variation> variations) {
		this.variations = variations;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public boolean isTemporary() {
		return temporary;
	}

	public void setTemporary(boolean temporary) {
		this.temporary = temporary;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public Instant getArchivedAt() {
		return archivedAt;
	}

	public void setArchivedAt(Instant archivedAt) {
		this.archivedAt = archivedAt;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
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
