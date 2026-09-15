package com.featureflags.approvals;

import java.time.Instant;
import java.util.Map;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Requests;

public final class ApprovalValidator {

	private static final int COMMENT_MAX = 1000;

	public record ReviewInput(String comment, Instant scheduleFor) {
	}

	public static ReviewInput validateApprove(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String comment = readComment(input, errors);
		Instant scheduleFor = readScheduleFor(input, errors);

		errors.throwIfAny();

		return new ReviewInput(comment, scheduleFor);
	}

	public record RejectInput(String comment) {
	}

	public static RejectInput validateReject(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String comment = readComment(input, errors);

		errors.throwIfAny();

		return new RejectInput(comment);
	}

	private static String readComment(Map<String, Object> input, FieldErrors errors) {
		String comment = Requests.string(input, "comment");

		if (comment == null) {
			return "";
		}

		String trimmed = comment.trim();

		if (trimmed.length() > COMMENT_MAX) {
			errors.add("comment", Requests.tooBigString(COMMENT_MAX));
		}

		return trimmed;
	}

	private static Instant readScheduleFor(Map<String, Object> input, FieldErrors errors) {
		if (!input.containsKey("scheduleFor") || input.get("scheduleFor") == null) {
			return null;
		}

		Instant scheduleFor = Requests.date(input, "scheduleFor");

		if (scheduleFor == null) {
			errors.add("scheduleFor", "Invalid input: expected an ISO-8601 date-time");
		} else if (scheduleFor.isBefore(Instant.now())) {
			errors.add("scheduleFor", "Scheduled time must be in the future.");
		}

		return scheduleFor;
	}

	private ApprovalValidator() {
	}
}
