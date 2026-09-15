package com.featureflags.seed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import com.featureflags.approvals.ApprovalRequest;
import com.featureflags.audiences.AppUser;
import com.featureflags.audiences.Segment;
import com.featureflags.audiences.SegmentRule;
import com.featureflags.auth.Account;
import com.featureflags.environments.Environment;
import com.featureflags.experiments.ExperimentEvent;
import com.featureflags.flags.Clause;
import com.featureflags.flags.FeatureFlag;
import com.featureflags.flags.FlagEnvironmentConfig;
import com.featureflags.history.AuditLogEntry;
import com.featureflags.projects.Project;
import com.featureflags.flags.Rule;
import com.featureflags.flags.RolloutWeight;
import com.featureflags.flags.Target;
import com.featureflags.flags.Variation;

import at.favre.lib.crypto.bcrypt.BCrypt;

@SpringBootApplication(scanBasePackages = "com.featureflags")
@EntityScan("com.featureflags")
public class SeedRunner {

	private static final String SEPARATOR = "========================================";

	public static void main(String[] args) {
		var context = new SpringApplicationBuilder(SeedRunner.class).web(WebApplicationType.NONE)
				.logStartupInfo(false).run(args);

		try {
			new SeedRunner().seed(context.getBean(MongoTemplate.class));
		} catch (RuntimeException exception) {
			System.err.println();
			System.err.println("Seeding failed: " + exception.getMessage());
			exception.printStackTrace();
			SpringApplication.exit(context, () -> 1);
			System.exit(1);
		}

		System.out.println("Disconnected from MongoDB");
		SpringApplication.exit(context, () -> 0);
		System.exit(0);
	}

	private MongoTemplate mongo;

	void seed(MongoTemplate mongoTemplate) {
		this.mongo = mongoTemplate;

		System.out.println(SEPARATOR);
		System.out.println("Database Seeding");
		System.out.println(SEPARATOR);
		System.out.println();
		System.out.println("Connecting to MongoDB...");
		mongo.executeCommand(new Document("ping", 1));
		System.out.println("Connected to MongoDB");

		clearDatabase();

		List<Account> accounts = seedAccounts();
		Account alex = accounts.get(0);
		Account jordan = accounts.get(1);
		Account sam = accounts.get(2);
		Account taylor = accounts.get(3);
		Account priya = accounts.get(4);

		seedWebAppProject(alex, jordan, sam, taylor, priya);
		seedMobileAppProject(alex, priya);

		report(accounts);
	}

	// ------------------------------------------------------------------
	// Accounts
	// ------------------------------------------------------------------

	private List<Account> seedAccounts() {
		System.out.println();
		System.out.println("Seeding accounts...");

		String passwordHash = BCrypt.with(BCrypt.Version.VERSION_2B).hashToString(SeedData.PASSWORD_ROUNDS,
				SeedData.DEMO_PASSWORD.toCharArray());
		List<Account> accounts = new ArrayList<>();
		String[] colors = { "#6366f1", "#0ea5e9", "#f59e0b", "#10b981", "#ec4899" };
		int index = 0;

		for (SeedData.AccountRow row : SeedData.accounts()) {
			Account account = new Account();
			account.setName(row.name());
			account.setEmail(row.email());
			account.setPasswordHash(passwordHash);
			account.setRole(row.role());
			account.setAvatarColor(colors[index % colors.length]);
			accounts.add(mongo.insert(account));
			index += 1;
		}

		System.out.println("  Created " + accounts.size() + " accounts");

		return accounts;
	}

	// ------------------------------------------------------------------
	// Web Application project - the deep demo with every capability
	// ------------------------------------------------------------------

	private void seedWebAppProject(Account alex, Account jordan, Account sam, Account taylor, Account priya) {
		Project project = insertProject("Consumer Web App", "web-app",
				"The main customer-facing web application.");
		Environment development = insertEnvironment(project, "Development", "development", "#22c55e", false, 0);
		Environment staging = insertEnvironment(project, "Staging", "staging", "#f59e0b", false, 1);
		Environment production = insertEnvironment(project, "Production", "production", "#ef4444", true, 2);

		List<AppUser> users = seedUsers(project, "web", 26);
		Segment enterpriseSegment = insertSegment(project, "Enterprise Customers", "enterprise-customers",
				"Accounts on the Enterprise plan.", List.of(rule("plan", "in", List.of("enterprise"))), List.of(),
				List.of());
		List<String> betaKeys = List.of(users.get(1).getKey(), users.get(5).getKey(), users.get(9).getKey());
		Segment betaSegment = insertSegment(project, "Beta Testers", "beta-testers",
				"Opted-in beta testers plus a rule on the betaOptIn attribute.",
				List.of(rule("betaOptIn", "equals", List.of("true"))), betaKeys, List.of());
		insertSegment(project, "EU Users", "eu-users", "Users located in the European Union.",
				List.of(rule("country", "in", List.of("GB", "DE", "FR"))), List.of(), List.of());

		seedNewCheckoutFlow(project, development, staging, production, priya, taylor);
		seedDarkModeUi(project, development, staging, production, betaSegment);
		seedPricingExperiment(project, development, staging, production, users);
		seedAiRecommendations(project, development, staging, production, jordan);
		seedSearchAutocomplete(project, development, staging, production, alex, jordan);
		seedEnterpriseSso(project, development, staging, production, enterpriseSegment);
		seedHolidayBanner(project, development, staging, production, sam, alex);
	}

	private void seedNewCheckoutFlow(Project project, Environment dev, Environment staging, Environment production,
			Account priya, Account taylor) {
		FeatureFlag flag = insertBooleanFlag(project, "new-checkout-flow", "New Checkout Flow",
				"A streamlined, single-page checkout replacing the old multi-step flow.",
				List.of("checkout", "rollout"), true, null);

		saveConfig(onConfig(flag, dev, "true"));
		saveConfig(onConfig(flag, staging, "true"));

		FlagEnvironmentConfig prodConfig = newConfig(flag, production);
		prodConfig.setEnabled(true);
		prodConfig.setOffVariationId("false");
		prodConfig.setFallthroughRollout(List.of(weight("true", 25), weight("false", 75)));
		saveConfig(prodConfig);

		history(project, flag, null, null, "FLAG_CREATED", "Priya Patel created flag \"New Checkout Flow\".", null,
				flag.toMap(), priya, daysAgo(21));
		history(project, flag, production, "CONFIG_UPDATED",
				"Priya Patel enabled New Checkout Flow at a 10% rollout in Production.", null, null, priya,
				daysAgo(14));
		history(project, flag, production, "CONFIG_UPDATED",
				"Priya Patel raised the Production rollout from 10% to 25%.", null, null, priya, daysAgo(6));

		ApprovalRequest rejected = insertApproval(project, flag, production, priya,
				Map.of("enabled", true, "offVariationId", "false", "targets", List.of(), "rules", List.of(),
						"fallthroughRollout", List.of(Map.of("variationId", "true", "weight", 100.0))),
				"Ready to go to 100% - conversion has held steady for two weeks.", ApprovalRequest.STATUS_REJECTED,
				taylor, "Let's wait for one more week of conversion data before going to 100%.", daysAgo(2));
		touchTimestamps(rejected, daysAgo(2));
	}

	private void seedDarkModeUi(Project project, Environment dev, Environment staging, Environment production,
			Segment betaSegment) {
		FeatureFlag flag = insertBooleanFlag(project, "dark-mode-ui", "Dark Mode UI",
				"A dark color theme across the app shell.", List.of("ui", "theme"), false, null);

		saveConfig(onConfig(flag, dev, "true"));
		saveConfig(onConfig(flag, staging, "true"));

		FlagEnvironmentConfig prodConfig = newConfig(flag, production);
		prodConfig.setEnabled(true);
		prodConfig.setOffVariationId("false");
		prodConfig.setRules(List.of(rule("r1", "Beta testers see dark mode first",
				List.of(segmentClause(betaSegment.getKey())), "true", null)));
		prodConfig.setFallthroughVariationId("false");
		saveConfig(prodConfig);
	}

	private void seedPricingExperiment(Project project, Environment dev, Environment staging, Environment production,
			List<AppUser> users) {
		List<Variation> variations = List.of(variation("control", "Control", "The current pricing page."),
				variation("treatment-a", "Treatment A", "Simplified 3-tier layout."),
				variation("treatment-b", "Treatment B", "Adds an annual-billing discount callout."));
		FeatureFlag flag = insertMultivariateFlag(project, "pricing-page-redesign", "Pricing Page Redesign",
				"A/B/C test of the pricing page layout.", List.of("experiment", "pricing"), true, variations);

		saveConfig(fallthroughOnly(flag, dev, "control"));
		saveConfig(fallthroughOnly(flag, staging, "control"));

		FlagEnvironmentConfig prodConfig = newConfig(flag, production);
		prodConfig.setEnabled(true);
		prodConfig.setFallthroughRollout(
				List.of(weight("control", 34), weight("treatment-a", 33), weight("treatment-b", 33)));
		saveConfig(prodConfig);

		seedPricingExperimentEvents(flag, production, users);
	}

	private void seedPricingExperimentEvents(FeatureFlag flag, Environment production, List<AppUser> users) {
		String[] variationIds = { "control", "treatment-a", "treatment-b" };
		// Treatment B is the deliberate "winner" so the comparison dashboard has a clear, significant signal.
		double[] conversionRates = { 0.20, 0.22, 0.31 };
		int syntheticVisitors = 420;
		java.util.Random random = new java.util.Random(42);

		for (int index = 0; index < syntheticVisitors; index += 1) {
			String userKey = index < users.size() ? users.get(index).getKey() : "pricing-visitor-" + index;
			String variationId = variationIds[index % variationIds.length];
			Instant exposedAt = daysAgo(14 - (index % 13));
			insertEvent(flag, production, userKey, variationId, ExperimentEvent.TYPE_EXPOSURE, null, null, exposedAt);

			double rate = conversionRates[index % variationIds.length];

			if (random.nextDouble() < rate) {
				double value = 20 + random.nextInt(40);
				insertEvent(flag, production, userKey, variationId, ExperimentEvent.TYPE_CONVERSION,
						"checkout_completed", value, exposedAt.plus(1, ChronoUnit.HOURS));
			}
		}
	}

	private void seedAiRecommendations(Project project, Environment dev, Environment staging, Environment production,
			Account jordan) {
		FeatureFlag flag = insertBooleanFlag(project, "ai-recommendations", "AI Recommendations",
				"Machine-learning powered \"recommended for you\" rail on the homepage.", List.of("ml", "homepage"),
				true, null);

		saveConfig(onConfig(flag, dev, "true"));
		saveConfig(onConfig(flag, staging, "true"));
		saveConfig(offConfig(flag, production, "false"));

		insertApproval(project, flag, production, jordan,
				Map.of("enabled", true, "offVariationId", "false", "targets", List.of(), "rules", List.of(),
						"fallthroughRollout", List.of(Map.of("variationId", "true", "weight", 10.0),
								Map.of("variationId", "false", "weight", 90.0))),
				"Model has passed offline eval, requesting a 10% Production rollout to gather live feedback.",
				ApprovalRequest.STATUS_PENDING, null, null, daysAgo(1));
	}

	private void seedSearchAutocomplete(Project project, Environment dev, Environment staging,
			Environment production, Account alex, Account jordan) {
		FeatureFlag flag = insertBooleanFlag(project, "search-autocomplete", "Search Autocomplete",
				"Inline suggestions as the user types in global search.", List.of("search"), false, null);

		saveConfig(onConfig(flag, dev, "true"));
		saveConfig(onConfig(flag, staging, "true"));

		FlagEnvironmentConfig earlyProd = newConfig(flag, production);
		earlyProd.setEnabled(true);
		earlyProd.setFallthroughRollout(List.of(weight("true", 50), weight("false", 50)));
		earlyProd.setVersion(2);
		Map<String, Object> earlySnapshot = earlyProd.toMap();

		FlagEnvironmentConfig currentProd = newConfig(flag, production);
		currentProd.setEnabled(true);
		currentProd.setOffVariationId("false");
		currentProd.setFallthroughVariationId("true");
		currentProd.setVersion(4);
		saveConfig(currentProd);

		history(project, flag, null, null, "FLAG_CREATED", "Jordan Lee created flag \"Search Autocomplete\".", null,
				flag.toMap(), jordan, daysAgo(30));
		history(project, flag, production, "CHANGE_PROPOSED",
				"Jordan Lee proposed a change to Production that requires approval before it goes live.", null, null,
				jordan, daysAgo(24));
		history(project, flag, production, "CONFIG_UPDATED", "Alex Morgan applied the approved change.", null,
				earlySnapshot, alex, daysAgo(23));
		history(project, flag, production, "CONFIG_UPDATED",
				"Alex Morgan raised the Production rollout from 50% to 100%.", earlySnapshot, currentProd.toMap(),
				alex, daysAgo(9));
	}

	private void seedEnterpriseSso(Project project, Environment dev, Environment staging, Environment production,
			Segment enterpriseSegment) {
		FeatureFlag flag = insertBooleanFlag(project, "enterprise-sso", "Enterprise SSO",
				"SAML single sign-on for Enterprise-plan accounts.", List.of("auth", "enterprise"), false, null);

		saveConfig(onConfig(flag, dev, "true"));
		saveConfig(onConfig(flag, staging, "true"));

		FlagEnvironmentConfig prodConfig = newConfig(flag, production);
		prodConfig.setEnabled(true);
		prodConfig.setOffVariationId("false");
		prodConfig.setRules(List.of(rule("r1", "Enterprise customers get SSO",
				List.of(segmentClause(enterpriseSegment.getKey())), "true", null)));
		prodConfig.setFallthroughVariationId("false");
		saveConfig(prodConfig);
	}

	private void seedHolidayBanner(Project project, Environment dev, Environment staging, Environment production,
			Account sam, Account alex) {
		List<Variation> variations = List.of(variation("off", "Off", "No banner."),
				variation("banner-v1", "Banner v1", "Static holiday banner."),
				variation("banner-v2", "Banner v2", "Animated banner with countdown timer."));
		FeatureFlag flag = insertMultivariateFlag(project, "holiday-promo-banner", "Holiday Promo Banner",
				"Seasonal promotional banner on the homepage.", List.of("marketing", "seasonal"), true, variations);

		saveConfig(fallthroughOnly(flag, dev, "banner-v1"));
		saveConfig(fallthroughOnly(flag, staging, "banner-v1"));
		saveConfig(fallthroughOnly(flag, production, "off"));

		insertApproval(project, flag, production, sam,
				Map.of("enabled", true, "offVariationId", "off", "targets", List.of(), "rules", List.of(),
						"fallthroughVariationId", "banner-v2"),
				"Turning on the animated banner for the winter sale launch.", ApprovalRequest.STATUS_SCHEDULED, alex,
				"Approved - let's line it up with the marketing email send.", in(2, ChronoUnit.DAYS), daysAgo(1));
	}

	// ------------------------------------------------------------------
	// Mobile Application project - lighter second project
	// ------------------------------------------------------------------

	private void seedMobileAppProject(Account alex, Account priya) {
		Project project = insertProject("Mobile Application", "mobile-app", "iOS and Android companion app.");
		Environment development = insertEnvironment(project, "Development", "development", "#22c55e", false, 0);
		Environment staging = insertEnvironment(project, "Staging", "staging", "#f59e0b", false, 1);
		Environment production = insertEnvironment(project, "Production", "production", "#ef4444", true, 2);

		List<AppUser> users = seedUsers(project, "mobile", 10);
		insertSegment(project, "Power Users", "power-users", "Users who opted into the beta program.",
				List.of(rule("betaOptIn", "equals", List.of("true"))), List.of(), List.of());

		FeatureFlag pushFlag = insertBooleanFlag(project, "push-notification-nudges", "Push Notification Nudges",
				"Re-engagement push notifications for inactive users.", List.of("growth"), true, null);
		saveConfig(onConfig(pushFlag, development, "true"));
		saveConfig(onConfig(pushFlag, staging, "true"));
		FlagEnvironmentConfig pushProd = newConfig(pushFlag, production);
		pushProd.setEnabled(true);
		pushProd.setFallthroughRollout(List.of(weight("true", 40), weight("false", 60)));
		saveConfig(pushProd);
		history(project, pushFlag, null, null, "FLAG_CREATED", "Alex Morgan created flag \"Push Notification Nudges\".",
				null, pushFlag.toMap(), alex, daysAgo(12));

		FeatureFlag offlineFlag = insertBooleanFlag(project, "offline-mode", "Offline Mode",
				"Cache recent content so the app stays usable without a connection.", List.of("reliability"), false,
				null);
		saveConfig(onConfig(offlineFlag, development, "true"));
		saveConfig(offConfig(offlineFlag, staging, "false"));
		saveConfig(offConfig(offlineFlag, production, "false"));

		System.out.println("  Seeded " + users.size() + " mobile app users across 2 flags");
		if (priya == null) {
			// keep the parameter meaningful for future per-account mobile fixtures without an unused-var warning
			throw new IllegalStateException("priya account is required for seeding");
		}
	}

	// ------------------------------------------------------------------
	// Shared builders
	// ------------------------------------------------------------------

	private List<AppUser> seedUsers(Project project, String prefix, int count) {
		List<AppUser> inserted = new ArrayList<>();

		for (SeedData.UserRow row : SeedData.users(prefix, count)) {
			AppUser user = new AppUser();
			user.setProjectId(project.getId());
			user.setKey(row.key());
			user.setName(row.name());
			user.setEmail(row.email());
			user.setPlan(row.plan());
			user.setCountry(row.country());
			Map<String, Object> attributes = new LinkedHashMap<>();
			attributes.put("betaOptIn", row.betaOptIn());
			attributes.put("signupCohort", row.cohort());
			user.setAttributes(attributes);
			inserted.add(mongo.insert(user));
		}

		return inserted;
	}

	private Project insertProject(String name, String key, String description) {
		Project project = new Project();
		project.setName(name);
		project.setKey(key);
		project.setDescription(description);

		return mongo.insert(project);
	}

	private Environment insertEnvironment(Project project, String name, String key, String color, boolean production,
			int sortOrder) {
		Environment environment = new Environment();
		environment.setProjectId(project.getId());
		environment.setName(name);
		environment.setKey(key);
		environment.setColor(color);
		environment.setProduction(production);
		environment.setSortOrder(sortOrder);

		return mongo.insert(environment);
	}

	private Segment insertSegment(Project project, String name, String key, String description,
			List<SegmentRule> rules, List<String> includedKeys, List<String> excludedKeys) {
		Segment segment = new Segment();
		segment.setProjectId(project.getId());
		segment.setName(name);
		segment.setKey(key);
		segment.setDescription(description);
		segment.setRules(rules);
		segment.setIncludedKeys(includedKeys);
		segment.setExcludedKeys(excludedKeys);

		return mongo.insert(segment);
	}

	private FeatureFlag insertBooleanFlag(Project project, String key, String name, String description,
			List<String> tags, boolean temporary, Account createdBy) {
		FeatureFlag flag = new FeatureFlag();
		flag.setProjectId(project.getId());
		flag.setKey(key);
		flag.setName(name);
		flag.setDescription(description);
		flag.setFlagType(FeatureFlag.TYPE_BOOLEAN);
		flag.setVariations(List.of(variation("true", "true", "On", ""), variation("false", "false", "Off", "")));
		flag.setTags(tags);
		flag.setTemporary(temporary);
		flag.setCreatedBy(createdBy == null ? null : createdBy.getId());

		return mongo.insert(flag);
	}

	private FeatureFlag insertMultivariateFlag(Project project, String key, String name, String description,
			List<String> tags, boolean temporary, List<Variation> variations) {
		FeatureFlag flag = new FeatureFlag();
		flag.setProjectId(project.getId());
		flag.setKey(key);
		flag.setName(name);
		flag.setDescription(description);
		flag.setFlagType(FeatureFlag.TYPE_MULTIVARIATE);
		flag.setVariations(variations);
		flag.setTags(tags);
		flag.setTemporary(temporary);

		return mongo.insert(flag);
	}

	private FlagEnvironmentConfig newConfig(FeatureFlag flag, Environment environment) {
		FlagEnvironmentConfig config = new FlagEnvironmentConfig();
		config.setId(newId());
		config.setFlagId(flag.getId());
		config.setEnvironmentId(environment.getId());
		config.setEnvironmentKey(environment.getKey());

		return config;
	}

	private FlagEnvironmentConfig onConfig(FeatureFlag flag, Environment environment, String variationId) {
		FlagEnvironmentConfig config = newConfig(flag, environment);
		config.setEnabled(true);
		config.setOffVariationId(variationId);
		config.setFallthroughVariationId(variationId);

		return config;
	}

	private FlagEnvironmentConfig offConfig(FeatureFlag flag, Environment environment, String offVariationId) {
		FlagEnvironmentConfig config = newConfig(flag, environment);
		config.setEnabled(false);
		config.setOffVariationId(offVariationId);
		config.setFallthroughVariationId(offVariationId);

		return config;
	}

	private FlagEnvironmentConfig fallthroughOnly(FeatureFlag flag, Environment environment, String variationId) {
		FlagEnvironmentConfig config = newConfig(flag, environment);
		config.setEnabled(true);
		config.setFallthroughVariationId(variationId);

		return config;
	}

	private void saveConfig(FlagEnvironmentConfig config) {
		mongo.save(config);
	}

	private Variation variation(String id, String value, String name, String description) {
		Variation variation = new Variation();
		variation.setId(id);
		variation.setValue(value);
		variation.setName(name);
		variation.setDescription(description);

		return variation;
	}

	private Variation variation(String id, String name, String description) {
		return variation(id, id, name, description);
	}

	private RolloutWeight weight(String variationId, double percent) {
		RolloutWeight rolloutWeight = new RolloutWeight();
		rolloutWeight.setVariationId(variationId);
		rolloutWeight.setWeight(percent);

		return rolloutWeight;
	}

	private SegmentRule rule(String attribute, String operator, List<String> values) {
		SegmentRule segmentRule = new SegmentRule();
		segmentRule.setAttribute(attribute);
		segmentRule.setOperator(operator);
		segmentRule.setValues(values);

		return segmentRule;
	}

	private Clause segmentClause(String... segmentKeys) {
		Clause clause = new Clause();
		clause.setOperator("segmentMatch");
		clause.setValues(List.of(segmentKeys));

		return clause;
	}

	private Rule rule(String id, String description, List<Clause> clauses, String variationId,
			List<RolloutWeight> rollout) {
		Rule flagRule = new Rule();
		flagRule.setId(id);
		flagRule.setDescription(description);
		flagRule.setClauses(clauses);
		flagRule.setVariationId(variationId);
		flagRule.setRollout(rollout == null ? List.of() : rollout);

		return flagRule;
	}

	private void history(Project project, FeatureFlag flag, Environment environment, String action, String summary,
			Map<String, Object> before, Map<String, Object> after, Account actor, Instant createdAt) {
		history(project, flag, environment == null ? null : environment.getId(),
				environment == null ? null : environment.getKey(), action, summary, before, after, actor, createdAt);
	}

	private void history(Project project, FeatureFlag flag, String environmentId, String environmentKey,
			String action, String summary, Map<String, Object> before, Map<String, Object> after, Account actor,
			Instant createdAt) {
		AuditLogEntry entry = new AuditLogEntry();
		entry.setId(newId());
		entry.setProjectId(project.getId());
		entry.setFlagId(flag.getId());
		entry.setEnvironmentId(environmentId);
		entry.setEnvironmentKey(environmentKey);
		entry.setAction(action);
		entry.setSummary(summary);
		entry.setBeforeSnapshot(before);
		entry.setAfterSnapshot(after);
		entry.setActorId(actor == null ? null : actor.getId());
		entry.setActorName(actor == null ? "System" : actor.getName());
		entry.setCreatedAt(createdAt);
		mongo.insert(entry);
	}

	private ApprovalRequest insertApproval(Project project, FeatureFlag flag, Environment environment,
			Account requester, Map<String, Object> proposedChange, String reason, String status, Account reviewer,
			String reviewComment, Instant createdAt) {
		return insertApproval(project, flag, environment, requester, proposedChange, reason, status, reviewer,
				reviewComment, null, createdAt);
	}

	private ApprovalRequest insertApproval(Project project, FeatureFlag flag, Environment environment,
			Account requester, Map<String, Object> proposedChange, String reason, String status, Account reviewer,
			String reviewComment, Instant scheduledFor, Instant createdAt) {
		ApprovalRequest request = new ApprovalRequest();
		request.setId(newId());
		request.setProjectId(project.getId());
		request.setFlagId(flag.getId());
		request.setFlagName(flag.getName());
		request.setEnvironmentId(environment.getId());
		request.setEnvironmentKey(environment.getKey());
		request.setEnvironmentName(environment.getName());
		request.setRequestedBy(requester.getId());
		request.setRequestedByName(requester.getName());
		request.setProposedChange(proposedChange);
		request.setReason(reason);
		request.setStatus(status);

		if (reviewer != null) {
			request.setReviewerId(reviewer.getId());
			request.setReviewerName(reviewer.getName());
			request.setReviewComment(reviewComment);
			request.setReviewedAt(createdAt);
		}

		request.setScheduledFor(scheduledFor);
		request.setCreatedAt(createdAt);
		request.setUpdatedAt(createdAt);

		return mongo.insert(request);
	}

	private void touchTimestamps(ApprovalRequest request, Instant when) {
		request.setUpdatedAt(when);
		mongo.save(request);
	}

	private void insertEvent(FeatureFlag flag, Environment environment, String userKey, String variationId,
			String eventType, String metricKey, Double value, Instant createdAt) {
		ExperimentEvent event = new ExperimentEvent();
		event.setId(newId());
		event.setFlagId(flag.getId());
		event.setEnvironmentId(environment.getId());
		event.setEnvironmentKey(environment.getKey());
		event.setUserKey(userKey);
		event.setVariationId(variationId);
		event.setEventType(eventType);
		event.setMetricKey(metricKey);
		event.setValue(value);
		event.setCreatedAt(createdAt);
		mongo.insert(event);
	}

	private Instant daysAgo(int days) {
		return Instant.now().minus(days, ChronoUnit.DAYS);
	}

	private Instant in(int amount, ChronoUnit unit) {
		return Instant.now().plus(amount, unit);
	}

	private String newId() {
		return new ObjectId().toHexString();
	}

	private void clearDatabase() {
		System.out.println("Clearing existing collections...");

		for (Class<?> type : List.of(ExperimentEvent.class, AuditLogEntry.class, ApprovalRequest.class,
				FlagEnvironmentConfig.class, FeatureFlag.class, Segment.class, AppUser.class, Environment.class,
				Project.class, Account.class)) {
			mongo.remove(new Query(), type);
		}
	}

	private void report(List<Account> accounts) {
		System.out.println();
		System.out.println(SEPARATOR);
		System.out.println("Seeding completed successfully!");
		System.out.println(SEPARATOR);
		System.out.println();
		System.out.println("Collection counts:");
		System.out.println("  Accounts:               " + mongo.getCollection("accounts").countDocuments());
		System.out.println("  Projects:               " + mongo.getCollection("projects").countDocuments());
		System.out.println("  Environments:           " + mongo.getCollection("environments").countDocuments());
		System.out.println("  App users:              " + mongo.getCollection("app_users").countDocuments());
		System.out.println("  Segments:               " + mongo.getCollection("segments").countDocuments());
		System.out.println("  Feature flags:          " + mongo.getCollection("feature_flags").countDocuments());
		System.out.println("  Flag environment configs: " + mongo.getCollection("flag_environment_configs").countDocuments());
		System.out.println("  Approval requests:      " + mongo.getCollection("approval_requests").countDocuments());
		System.out.println("  Experiment events:      " + mongo.getCollection("experiment_events").countDocuments());
		System.out.println("  Audit log entries:      " + mongo.getCollection("audit_log_entries").countDocuments());
		System.out.println();
		System.out.println("Demo accounts (all use the same password):");

		for (Account account : accounts) {
			System.out.println("  Email: " + account.getEmail() + " | Password: " + SeedData.DEMO_PASSWORD);
		}

		System.out.println();
		System.out.println(SEPARATOR);
		System.out.println();
	}
}
