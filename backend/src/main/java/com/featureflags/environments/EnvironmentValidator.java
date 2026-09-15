package com.featureflags.environments;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.featureflags.projects.ProjectKeys;
import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class EnvironmentValidator {

	private static final Pattern KEY = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

	private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

	private static final int NAME_MAX = 60;

	public record CreateInput(String name, String key, String color, boolean production) {
	}

	public static CreateInput validateCreate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String name = readName(input, errors);
		String key = readKey(input, errors, name);
		String color = readColor(input, errors);
		Boolean production = Requests.bool(input, "production");

		errors.throwIfAny();

		return new CreateInput(name, key, color, production != null && production);
	}

	public static Map<String, Object> validateUpdate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		Map<String, Object> updates = new LinkedHashMap<>();

		if (input.containsKey("name")) {
			updates.put("name", readName(input, errors));
		}

		if (input.containsKey("color")) {
			updates.put("color", readColor(input, errors));
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
			errors.add("key", "Use lowercase letters, numbers, and hyphens only.");
		}

		return candidate;
	}

	private static String readColor(Map<String, Object> input, FieldErrors errors) {
		String color = Requests.string(input, "color");

		if (color == null) {
			return "#6366f1";
		}

		if (!COLOR.matcher(color).matches()) {
			errors.add("color", "Invalid string: must match pattern /^#[0-9A-Fa-f]{6}$/");
		}

		return color;
	}

	private EnvironmentValidator() {
	}
}
