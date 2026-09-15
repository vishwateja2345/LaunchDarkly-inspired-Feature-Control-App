package com.featureflags.audiences;

import java.util.LinkedHashMap;
import java.util.Map;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class AppUserValidator {

	private static final int KEY_MAX = 120;

	private static final int NAME_MAX = 120;

	public record CreateInput(String key, String name, String email, String plan, String country,
			Map<String, Object> attributes) {
	}

	public static CreateInput validateCreate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String key = readKey(input, errors);
		String name = readRequiredString(input, errors, "name", NAME_MAX);
		String email = Requests.string(input, "email");
		String plan = Requests.string(input, "plan");
		String country = Requests.string(input, "country");
		Map<String, Object> attributes = Requests.map(input, "attributes");

		errors.throwIfAny();

		return new CreateInput(key, name, email == null ? "" : email.trim(), plan == null ? "free" : plan,
				country == null ? "US" : country, attributes == null ? Map.of() : attributes);
	}

	public static Map<String, Object> validateUpdate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		Map<String, Object> updates = new LinkedHashMap<>();

		if (input.containsKey("name")) {
			updates.put("name", readRequiredString(input, errors, "name", NAME_MAX));
		}

		if (input.containsKey("email")) {
			updates.put("email", Requests.string(input, "email"));
		}

		if (input.containsKey("plan")) {
			updates.put("plan", Requests.string(input, "plan"));
		}

		if (input.containsKey("country")) {
			updates.put("country", Requests.string(input, "country"));
		}

		if (input.containsKey("attributes")) {
			Map<String, Object> attributes = Requests.map(input, "attributes");
			updates.put("attributes", attributes == null ? Map.of() : attributes);
		}

		if (updates.isEmpty() && errors.isEmpty()) {
			errors.addFormError("Provide at least one field to update.");
		}

		errors.throwIfAny();

		return updates;
	}

	private static String readKey(Map<String, Object> input, FieldErrors errors) {
		String key = Requests.string(input, "key");

		if (key == null || key.trim().isEmpty()) {
			errors.add("key", Messages.MISSING_STRING);

			return null;
		}

		String trimmed = key.trim();

		if (trimmed.length() > KEY_MAX) {
			errors.add("key", Requests.tooBigString(KEY_MAX));
		}

		return trimmed;
	}

	private static String readRequiredString(Map<String, Object> input, FieldErrors errors, String field, int max) {
		String value = Requests.string(input, field);

		if (value == null) {
			errors.add(field, Messages.MISSING_STRING);

			return null;
		}

		String trimmed = value.trim();

		if (trimmed.isEmpty()) {
			errors.add(field, Requests.tooSmallString(1));
		} else if (trimmed.length() > max) {
			errors.add(field, Requests.tooBigString(max));
		}

		return trimmed;
	}

	private AppUserValidator() {
	}
}
