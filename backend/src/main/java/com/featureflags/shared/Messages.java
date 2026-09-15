package com.featureflags.shared;

import java.util.List;
import java.util.stream.Collectors;

public final class Messages {

	public static final String MISSING_STRING = "Invalid input: expected string, received undefined";

	public static final String MISSING_NUMBER = "Invalid input: expected number, received NaN";

	public static final String MISSING_ARRAY = "Invalid input: expected array, received undefined";

	public static final String INVALID_EMAIL = "Invalid email address";

	public static String tooSmallArray(int minimum) {
		return "Too small: expected array to have >=" + minimum + " items";
	}

	public static String tooBigArray(int maximum) {
		return "Too big: expected array to have <=" + maximum + " items";
	}

	public static String tooSmallNumber(int minimum) {
		return "Too small: expected number to be >=" + minimum;
	}

	public static String tooBigNumber(int maximum) {
		return "Too big: expected number to be <=" + maximum;
	}

	public static String invalidOption(List<String> options) {
		return "Invalid option: expected one of "
				+ options.stream().map(option -> "\"" + option + "\"").collect(Collectors.joining("|"));
	}

	public static String expected(String type, String received) {
		return "Invalid input: expected " + type + ", received " + received;
	}

	private Messages() {
	}
}
