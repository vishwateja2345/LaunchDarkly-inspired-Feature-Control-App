package com.featureflags.auth;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import com.featureflags.shared.Dates;

@Document(collection = "accounts")
public class Account {

	public static final String ROLE_ADMIN = "admin";

	public static final String ROLE_MEMBER = "member";

	@Id
	private String id;

	private String name;

	private String email;

	private String passwordHash;

	private String role = ROLE_MEMBER;

	private String avatarColor = "#6366f1";

	private boolean active = true;

	@CreatedDate
	private Instant createdAt;

	@LastModifiedDate
	private Instant updatedAt;

	public Map<String, Object> toPublicMap() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("_id", id);
		body.put("name", name);
		body.put("email", email);
		body.put("role", role);
		body.put("avatarColor", avatarColor);
		body.put("createdAt", Dates.iso(createdAt));

		return body;
	}

	public boolean isAdmin() {
		return ROLE_ADMIN.equals(role);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getAvatarColor() {
		return avatarColor;
	}

	public void setAvatarColor(String avatarColor) {
		this.avatarColor = avatarColor;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
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
