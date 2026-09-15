package com.featureflags.flags;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Deterministic, sticky percentage bucketing. The same hash key always lands in the
 * same bucket, so a given user consistently gets the same rollout variation across
 * evaluations, matching how production feature-flag SDKs implement rollouts.
 */
public final class Bucketing {

	private static final long HASH_MODULO = 100_000L;

	private static final int HASH_HEX_DIGITS = 15;

	/** Returns a stable value in [0, 100) with three-decimal resolution for the given key. */
	public static double bucketFor(String hashKey) {
		long bucketValue = longHash(hashKey) % HASH_MODULO;

		return bucketValue / (HASH_MODULO / 100.0);
	}

	/** Picks a variation from weighted buckets (weights are percentages that should sum to ~100). */
	public static String pickVariation(String hashKey, List<RolloutWeight> weights) {
		if (weights.isEmpty()) {
			return null;
		}

		double bucket = bucketFor(hashKey);
		double cumulative = 0;

		for (RolloutWeight weight : weights) {
			cumulative += weight.getWeight();

			if (bucket < cumulative) {
				return weight.getVariationId();
			}
		}

		// Rounding safety net when weights sum to slightly under 100.
		return weights.get(weights.size() - 1).getVariationId();
	}

	private static long longHash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();

			for (int index = 0; index < 8; index += 1) {
				hex.append(String.format("%02x", hashed[index]));
			}

			return Long.parseLong(hex.substring(0, HASH_HEX_DIGITS), 16);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-1 is required for rollout bucketing.", exception);
		}
	}

	private Bucketing() {
	}
}
