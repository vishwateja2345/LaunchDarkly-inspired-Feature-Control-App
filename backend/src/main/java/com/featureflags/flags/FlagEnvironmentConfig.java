package com.featureflags.flags;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.featureflags.shared.Dates;

/**
 * A feature flag's configuration within a single environment: whether it is on,
 * individual user targets, ordered rules, and the fallthrough (default) resolution.
 * This is the unit that approvals gate and history snapshots.
 */
@Document(collection = "flag_environment_configs")
public class FlagEnvironmentConfig {

	@Id
	private String id;

	@Field(targetType = FieldType.OBJECT_ID)
	private String flagId;

	@Field(targetType = FieldType.OBJECT_ID)
	private String environmentId;

	private String environmentKey;

	private boolean enabled;

	private String offVariationId;

	private List<Target> targets = new ArrayList<>();

	private List<Rule> rules = new ArrayList<>();

	private String fallthroughVariationId;

	private List<RolloutWeight> fallthroughRollout = new ArrayList<>();

	private int version = 1;

	@Field(targetType = FieldType.OBJECT_ID)
	private String updatedBy;

	private Instant updatedAt = Instant.now();

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("flagId", flagId);
		body.put("environmentId", environmentId);
		body.put("environmentKey", environmentKey);
		body.put("enabled", enabled);
		body.put("offVariationId", offVariationId);
		body.put("targets", targets.stream().map(Target::toMap).toList());
		body.put("rules", rules.stream().map(Rule::toMap).toList());
		body.put("fallthroughVariationId", fallthroughVariationId);
		body.put("fallthroughRollout", fallthroughRollout.stream().map(RolloutWeight::toMap).toList());
		body.put("version", version);
		body.put("updatedBy", updatedBy);
		body.put("updatedAt", Dates.iso(updatedAt));

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

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getOffVariationId() {
		return offVariationId;
	}

	public void setOffVariationId(String offVariationId) {
		this.offVariationId = offVariationId;
	}

	public List<Target> getTargets() {
		return targets;
	}

	public void setTargets(List<Target> targets) {
		this.targets = targets;
	}

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	public String getFallthroughVariationId() {
		return fallthroughVariationId;
	}

	public void setFallthroughVariationId(String fallthroughVariationId) {
		this.fallthroughVariationId = fallthroughVariationId;
	}

	public List<RolloutWeight> getFallthroughRollout() {
		return fallthroughRollout;
	}

	public void setFallthroughRollout(List<RolloutWeight> fallthroughRollout) {
		this.fallthroughRollout = fallthroughRollout;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
