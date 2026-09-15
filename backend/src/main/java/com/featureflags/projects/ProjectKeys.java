package com.featureflags.projects;

import java.util.regex.Pattern;

public final class ProjectKeys {

	private static final Pattern SLUG = Pattern.compile("[^a-z0-9]+");

	public static String slugify(String value) {
		String lowered = value.trim().toLowerCase();
		String slug = SLUG.matcher(lowered).replaceAll("-");

		return trimDashes(slug);
	}

	private static String trimDashes(String value) {
		int start = 0;
		int end = value.length();

		while (start < end && value.charAt(start) == '-') {
			start += 1;
		}

		while (end > start && value.charAt(end - 1) == '-') {
			end -= 1;
		}

		return value.substring(start, end);
	}

	private ProjectKeys() {
	}
}
