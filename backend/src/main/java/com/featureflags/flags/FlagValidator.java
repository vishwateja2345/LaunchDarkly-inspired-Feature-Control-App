package com.featureflags.flags;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.ObjectIds;
import com.featureflags.shared.Requests;

public final class FlagValidator {

	private static final Pattern KEY = Pattern.compile("^[a-z0-9][a-z0-9-_.]{0,99}$");

	private static final int NAME_MAX = 100;

	private static final int DESCRIPTION_MAX = 1000;

	private static final int VARIATIONS_MIN = 2;

	private static final int VARIATIONS_MAX = 20;

	public record CreateInput(String key, String name, String description, String flagType, List<Variation> variations,
			List<String> tags, boolean temporary) {
	}

	public static CreateInput validateCreate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String name = readName(input, errors);
		String key = readKey(input, errors, name);
		String description = readDescription(input, errors);
		String flagType = readFlagType(input, errors);
		List<Variation> variations = readVariations(input, errors, flagType);
		List<String> tags = Requests.stringList(input, "tags");
		Boolean temporary = Requests.bool(input, "temporary");

		errors.throwIfAny();

		return new CreateInput(key, name, description, flagType, variations, tags == null ? List.of() : tags,
				temporary == null || temporary);
	}

	public static Map<String, Object> validateUpdate(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		Map<String, Object> updates = new LinkedHashMap<>();

		if (input.containsKey("name")) {
			updates.put("name", readName(input, errors));
		}

		if (input.containsKey("description")) {
			updates.put("description", readDescription(input, errors));
		}

		if (input.containsKey("tags")) {
			List<String> tags = Requests.stringList(input, "tags");
			updates.put("tags", tags == null ? List.of() : tags);
		}

		if (input.containsKey("temporary")) {
			Boolean temporary = Requests.bool(input, "temporary");

			if (temporary == null) {
				errors.add("temporary", Messages.expected("boolean", "undefined"));
			} else {
				updates.put("temporary", temporary);
			}
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
		String candidate = key != null ? key.trim()
				: com.featureflags.projects.ProjectKeys.slugify(fallbackName == null ? "" : fallbackName);

		if (candidate.isEmpty()) {
			errors.add("key", Messages.MISSING_STRING);

			return null;
		}

		if (!KEY.matcher(candidate).matches()) {
			errors.add("key", "Use lowercase letters, numbers, hyphens, underscores, and dots only.");
		}

		return candidate;
	}

	private static String readDescription(Map<String, Object> input, FieldErrors errors) {
		String description = Requests.string(input, "description");

		if (description == null) {
			return "";
		}

		String trimmed = description.trim();

		if (trimmed.length() > DESCRIPTION_MAX) {
			errors.add("description", Requests.tooBigString(DESCRIPTION_MAX));
		}

		return trimmed;
	}

	private static String readFlagType(Map<String, Object> input, FieldErrors errors) {
		String flagType = Requests.string(input, "flagType");

		if (flagType == null) {
			return FeatureFlag.TYPE_BOOLEAN;
		}

		if (!FeatureFlag.TYPE_BOOLEAN.equals(flagType) && !FeatureFlag.TYPE_MULTIVARIATE.equals(flagType)) {
			errors.add("flagType", Messages.invalidOption(List.of(FeatureFlag.TYPE_BOOLEAN, FeatureFlag.TYPE_MULTIVARIATE)));
		}

		return flagType;
	}

	@SuppressWarnings("unchecked")
	private static List<Variation> readVariations(Map<String, Object> input, FieldErrors errors, String flagType) {
		if (FeatureFlag.TYPE_BOOLEAN.equals(flagType)) {
			List<Variation> variations = new ArrayList<>();
			variations.add(variation("true", "true", "On", ""));
			variations.add(variation("false", "false", "Off", ""));

			return variations;
		}

		List<Object> raw = Requests.list(input, "variations");

		if (raw == null || raw.size() < VARIATIONS_MIN) {
			errors.add("variations", Messages.tooSmallArray(VARIATIONS_MIN));

			return new ArrayList<>();
		}

		if (raw.size() > VARIATIONS_MAX) {
			errors.add("variations", Messages.tooBigArray(VARIATIONS_MAX));
		}

		List<Variation> variations = new ArrayList<>();
		Set<String> seenValues = new LinkedHashSet<>();

		for (int index = 0; index < raw.size(); index += 1) {
			if (!(raw.get(index) instanceof Map<?, ?> rawVariation)) {
				errors.add("variations[" + index + "]", Messages.expected("object", "undefined"));
				continue;
			}

			Map<String, Object> variationInput = (Map<String, Object>) rawVariation;
			String value = Requests.string(variationInput, "value");
			String name = Requests.string(variationInput, "name");
			String description = Requests.string(variationInput, "description");

			if (value == null || value.isBlank()) {
				errors.add("variations[" + index + "].value", Messages.MISSING_STRING);
			} else if (!seenValues.add(value)) {
				errors.add("variations[" + index + "].value", "Variation values must be unique.");
			}

			if (name == null || name.isBlank()) {
				errors.add("variations[" + index + "].name", Messages.MISSING_STRING);
			}

			variations.add(variation(ObjectIds.next(), value, name, description == null ? "" : description));
		}

		return variations;
	}

	private static Variation variation(String id, String value, String name, String description) {
		Variation variation = new Variation();
		variation.setId(id);
		variation.setValue(value);
		variation.setName(name);
		variation.setDescription(description);

		return variation;
	}

	private FlagValidator() {
	}
}
