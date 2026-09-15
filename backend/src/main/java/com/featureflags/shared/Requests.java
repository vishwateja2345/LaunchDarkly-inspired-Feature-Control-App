package com.featureflags.shared;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class Requests {

	private static final Pattern DATE_ONLY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

	public static Map<String, Object> body(Map<String, Object> body) {
		return body == null ? Map.of() : body;
	}

	public static Map<String, Object> query(Map<String, String> parameters) {
		return new LinkedHashMap<>(parameters);
	}

	public static boolean has(Map<String, Object> input, String key) {
		return input.get(key) != null;
	}

	public static String string(Map<String, Object> input, String key) {
		Object value = input.get(key);

		return value instanceof String text ? text : null;
	}

	public static Boolean bool(Map<String, Object> input, String key) {
		Object value = input.get(key);

		return value instanceof Boolean flag ? flag : null;
	}

	public static Integer integer(Map<String, Object> input, String key) {
		Object value = input.get(key);

		if (value instanceof Number number) {
			return number.intValue();
		}

		if (value instanceof String text) {
			try {
				return Integer.valueOf(text.trim());
			} catch (NumberFormatException exception) {
				return null;
			}
		}

		return null;
	}

	public static Double number(Map<String, Object> input, String key) {
		Object value = input.get(key);

		if (value instanceof Number number) {
			return number.doubleValue();
		}

		if (value instanceof String text) {
			try {
				return Double.valueOf(text.trim());
			} catch (NumberFormatException exception) {
				return null;
			}
		}

		return null;
	}

	public static Instant date(Map<String, Object> input, String key) {
		Object value = input.get(key);

		return value instanceof String text ? parseDate(text) : null;
	}

	private static Instant parseDate(String value) {
		try {
			if (DATE_ONLY.matcher(value).matches()) {
				return LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant();
			}

			return OffsetDateTime.parse(value).toInstant();
		} catch (RuntimeException exception) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Object> list(Map<String, Object> input, String key) {
		Object value = input.get(key);

		return value instanceof List<?> items ? new ArrayList<>((List<Object>) items) : null;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> map(Map<String, Object> input, String key) {
		Object value = input.get(key);

		return value instanceof Map<?, ?> items ? new LinkedHashMap<>((Map<String, Object>) items) : null;
	}

	public static List<String> stringList(Map<String, Object> input, String key) {
		List<Object> raw = list(input, key);

		if (raw == null) {
			return null;
		}

		return raw.stream().filter(item -> item instanceof String).map(item -> (String) item).toList();
	}

	public static String tooSmallString(int minimum) {
		return "Too small: expected string to have >=" + minimum + " characters";
	}

	public static String tooBigString(int maximum) {
		return "Too big: expected string to have <=" + maximum + " characters";
	}

	private Requests() {
	}
}
