package com.featureflags.projects;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class ProjectValidator {

	private static final Pattern KEY = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

	private static final int NAME_MAX = 80;

	private static final int DESCRIPTION_MAX = 1000;

	private static final String KEY_MESSAGE = "Use lowercase letters, numbers, and hyphens only.";

	public record CreateInput(String name, String key, String description) {
	}

	public static CreateInput validateCreate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String name = readName(input, errors);
		String key = readKey(input, errors, name);
		String description = readDescription(input, errors, "");

		errors.throwIfAny();

		return new CreateInput(name, key, description);
	}

	public static Map<String, Object> validateUpdate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		Map<String, Object> updates = new LinkedHashMap<>();

		if (input.containsKey("name")) {
			updates.put("name", readName(input, errors));
		}

		if (input.containsKey("description")) {
			updates.put("description", readDescription(input, errors, null));
		}

		if (updates.isEmpty() && errors.isEmpty()) {
			errors.addFormError("Provide at least one field to update.");
		}

		errors.throwIfAny();

		return updates;
	}

	private static String readName(Map<String, Object> input, FieldErrors errors) {
		String name = Requests.string(input, "name");

		if (name == null) {
			errors.add("name", Messages.MISSING_STRING);

			return null;
		}

		String trimmed = name.trim();

		if (trimmed.isEmpty()) {
			errors.add("name", Requests.tooSmallString(1));
		} else if (trimmed.length() > NAME_MAX) {
			errors.add("name", Requests.tooBigString(NAME_MAX));
		}

		return trimmed;
	}

	private static String readKey(Map<String, Object> input, FieldErrors errors, String fallbackName) {
		String key = Requests.string(input, "key");
		String candidate = key != null ? key.trim() : ProjectKeys.slugify(fallbackName == null ? "" : fallbackName);

		if (candidate.isEmpty()) {
			errors.add("key", Messages.MISSING_STRING);

			return null;
		}

		if (!KEY.matcher(candidate).matches()) {
			errors.add("key", KEY_MESSAGE);
		}

		return candidate;
	}

	private static String readDescription(Map<String, Object> input, FieldErrors errors, String fallback) {
		String description = Requests.string(input, "description");

		if (description == null) {
			return fallback;
		}

		String trimmed = description.trim();

		if (trimmed.length() > DESCRIPTION_MAX) {
			errors.add("description", Requests.tooBigString(DESCRIPTION_MAX));
		}

		return trimmed;
	}

	private ProjectValidator() {
	}
}
