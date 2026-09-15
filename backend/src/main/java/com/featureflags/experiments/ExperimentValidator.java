package com.featureflags.experiments;

import java.util.Map;

import com.featureflags.shared.FieldErrors;
import com.featureflags.shared.Messages;
import com.featureflags.shared.Requests;

public final class ExperimentValidator {

	public record ExposureInput(String userKey, String variationId) {
	}

	public record ConversionInput(String userKey, String metricKey, Double value) {
	}

	public static ExposureInput validateExposure(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String userKey = requireString(input, "userKey", errors);
		String variationId = requireString(input, "variationId", errors);

		errors.throwIfAny();

		return new ExposureInput(userKey, variationId);
	}

	public static ConversionInput validateConversion(Map<String, Object> body) {
		Map<String, Object> input = Requests.body(body);
		FieldErrors errors = new FieldErrors();
		String userKey = requireString(input, "userKey", errors);
		String metricKey = requireString(input, "metricKey", errors);
		Double value = Requests.number(input, "value");

		errors.throwIfAny();

		return new ConversionInput(userKey, metricKey, value);
	}

	private static String requireString(Map<String, Object> input, String field, FieldErrors errors) {
		String value = Requests.string(input, field);

		if (value == null || value.isBlank()) {
			errors.add(field, Messages.MISSING_STRING);
		}

		return value;
	}

	private ExperimentValidator() {
	}
}
