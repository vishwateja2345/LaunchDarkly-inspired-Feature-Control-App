package com.featureflags.seed;

import java.util.List;

/** Small fixed fixture lists shared by {@link SeedRunner}. */
final class SeedData {

	static final String DEMO_PASSWORD = "password123";

	static final int PASSWORD_ROUNDS = 10;

	record AccountRow(String name, String email, String role) {
	}

	static List<AccountRow> accounts() {
		return List.of(new AccountRow("Alex Morgan", "alex.morgan@flagdeck.com", "admin"),
				new AccountRow("Jordan Lee", "jordan.lee@flagdeck.com", "member"),
				new AccountRow("Sam Rivera", "sam.rivera@flagdeck.com", "member"),
				new AccountRow("Taylor Chen", "taylor.chen@flagdeck.com", "member"),
				new AccountRow("Priya Patel", "priya.patel@flagdeck.com", "member"));
	}

	record UserRow(String key, String name, String email, String plan, String country, boolean betaOptIn,
			String cohort) {
	}

	private static final String[] FIRST_NAMES = { "Morgan", "Riley", "Casey", "Drew", "Jamie", "Avery", "Quinn",
			"Reese", "Rowan", "Skyler", "Emerson", "Finley", "Harper", "Kendall", "Logan", "Parker", "Sawyer",
			"Shawn", "Tatum", "Blake", "Cameron", "Dakota", "Elliot", "Frankie" };

	private static final String[] LAST_NAMES = { "Diaz", "Nguyen", "Okafor", "Singh", "Rossi", "Kowalski", "Haddad",
			"Kim", "Silva", "Novak", "Fischer", "Suzuki", "Dubois", "Petrova", "Costa", "Andersen", "Osei", "Lund",
			"Moreno", "Park", "Haas", "Braga", "Weiss", "Alves" };

	private static final String[] COUNTRIES = { "US", "US", "US", "GB", "DE", "IN", "BR", "CA", "AU", "FR", "GB",
			"IN", "US", "DE", "BR", "US", "GB", "US", "IN", "CA", "FR", "DE", "US", "AU" };

	private static final String[] PLANS = { "free", "free", "pro", "pro", "enterprise", "free", "pro", "free",
			"enterprise", "pro", "free", "pro", "enterprise", "free", "pro", "free", "pro", "enterprise", "free",
			"pro", "free", "enterprise", "pro", "free" };

	/** Deterministic, varied roster of app users (the audience flags target - not logins). */
	static List<UserRow> users(String projectPrefix, int count) {
		List<UserRow> rows = new java.util.ArrayList<>();

		for (int index = 0; index < count; index += 1) {
			String first = FIRST_NAMES[index % FIRST_NAMES.length];
			String last = LAST_NAMES[index % LAST_NAMES.length];
			String key = projectPrefix + "-user-" + (index + 1);
			String email = (first + "." + last + index).toLowerCase() + "@example.com";
			boolean betaOptIn = index % 4 == 0;
			String cohort = index < 8 ? "2025-q4" : index < 16 ? "2026-q1" : "2026-q2";
			rows.add(new UserRow(key, first + " " + last, email, PLANS[index % PLANS.length],
					COUNTRIES[index % COUNTRIES.length], betaOptIn, cohort));
		}

		return rows;
	}

	private SeedData() {
	}
}
