package com.featureflags.approvals;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import com.featureflags.shared.Dates;

@Document(collection = "approval_requests")
public class ApprovalRequest {

	public static final String STATUS_PENDING = "pending";

	public static final String STATUS_SCHEDULED = "scheduled";

	public static final String STATUS_APPLIED = "applied";

	public static final String STATUS_REJECTED = "rejected";

	public static final String STATUS_CANCELLED = "cancelled";

	@Id
	private String id;

	@Field(targetType = FieldType.OBJECT_ID)
	private String projectId;

	@Field(targetType = FieldType.OBJECT_ID)
	private String flagId;

	private String flagName;

	@Field(targetType = FieldType.OBJECT_ID)
	private String environmentId;

	private String environmentKey;

	private String environmentName;

	@Field(targetType = FieldType.OBJECT_ID)
	private String requestedBy;

	private String requestedByName;

	private Map<String, Object> proposedChange;

	private String reason = "";

	/** The history action label to apply once this request is approved (e.g. "CONFIG_UPDATED", "ROLLED_BACK"). */
	private String changeAction = "CONFIG_UPDATED";

	private String status = STATUS_PENDING;

	@Field(targetType = FieldType.OBJECT_ID)
	private String reviewerId;

	private String reviewerName;

	private String reviewComment;

	private Instant reviewedAt;

	private Instant scheduledFor;

	@CreatedDate
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;

	public Map<String, Object> toMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("projectId", projectId);
		body.put("flagId", flagId);
		body.put("flagName", flagName);
		body.put("environmentId", environmentId);
		body.put("environmentKey", environmentKey);
		body.put("environmentName", environmentName);
		body.put("requestedBy", requestedBy);
		body.put("requestedByName", requestedByName);
		body.put("proposedChange", proposedChange);
		body.put("reason", reason);
		body.put("changeAction", changeAction);
		body.put("status", status);
		body.put("reviewerId", reviewerId);
		body.put("reviewerName", reviewerName);
		body.put("reviewComment", reviewComment);
		body.put("reviewedAt", Dates.iso(reviewedAt));
		body.put("scheduledFor", Dates.iso(scheduledFor));
		body.put("createdAt", Dates.iso(createdAt));
		body.put("updatedAt", Dates.iso(updatedAt));

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

	public String getFlagName() {
		return flagName;
	}

	public void setFlagName(String flagName) {
		this.flagName = flagName;
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

	public String getEnvironmentName() {
		return environmentName;
	}

	public void setEnvironmentName(String environmentName) {
		this.environmentName = environmentName;
	}

	public String getRequestedBy() {
		return requestedBy;
	}

	public void setRequestedBy(String requestedBy) {
		this.requestedBy = requestedBy;
	}

	public String getRequestedByName() {
		return requestedByName;
	}

	public void setRequestedByName(String requestedByName) {
		this.requestedByName = requestedByName;
	}

	public Map<String, Object> getProposedChange() {
		return proposedChange;
	}

	public void setProposedChange(Map<String, Object> proposedChange) {
		this.proposedChange = proposedChange;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getChangeAction() {
		return changeAction;
	}

	public void setChangeAction(String changeAction) {
		this.changeAction = changeAction == null ? "CONFIG_UPDATED" : changeAction;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getReviewerId() {
		return reviewerId;
	}

	public void setReviewerId(String reviewerId) {
		this.reviewerId = reviewerId;
	}

	public String getReviewerName() {
		return reviewerName;
	}

	public void setReviewerName(String reviewerName) {
		this.reviewerName = reviewerName;
	}

	public String getReviewComment() {
		return reviewComment;
	}

	public void setReviewComment(String reviewComment) {
		this.reviewComment = reviewComment;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(Instant reviewedAt) {
		this.reviewedAt = reviewedAt;
	}

	public Instant getScheduledFor() {
		return scheduledFor;
	}

	public void setScheduledFor(Instant scheduledFor) {
		this.scheduledFor = scheduledFor;
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
