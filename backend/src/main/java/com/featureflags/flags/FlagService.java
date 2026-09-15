package com.featureflags.flags;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.auth.Account;
import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.history.HistoryService;
import com.featureflags.projects.ProjectService;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.ObjectIds;

@Service
public class FlagService {

	private static final int NOT_FOUND = 404;

	private static final int CONFLICT = 409;

	private final FlagRepository flagRepository;

	private final FlagConfigRepository flagConfigRepository;

	private final ProjectService projectService;

	private final EnvironmentService environmentService;

	private final HistoryService historyService;

	public FlagService(FlagRepository flagRepository, FlagConfigRepository flagConfigRepository,
			ProjectService projectService, EnvironmentService environmentService, HistoryService historyService) {
		this.flagRepository = flagRepository;
		this.flagConfigRepository = flagConfigRepository;
		this.projectService = projectService;
		this.environmentService = environmentService;
		this.historyService = historyService;
	}

	public List<FeatureFlag> list(String projectId, boolean includeArchived) {
		projectService.requireProject(projectId);

		return flagRepository.listByProject(projectId, includeArchived);
	}

	public FeatureFlag get(String flagId) {
		return requireFlag(flagId);
	}

	public FeatureFlag requireByKey(String projectId, String flagKey) {
		FeatureFlag flag = flagRepository.findByKey(projectId, flagKey);

		if (flag == null) {
			throw new ApiException(NOT_FOUND, "FLAG_NOT_FOUND", "No flag with key \"" + flagKey + "\" exists in this project.");
		}

		return flag;
	}

	public FeatureFlag create(String projectId, FlagValidator.CreateInput input, Account actor) {
		projectService.requireProject(projectId);

		if (flagRepository.findByKey(projectId, input.key()) != null) {
			throw new ApiException(CONFLICT, "FLAG_KEY_CONFLICT", "A flag with this key already exists in this project.");
		}

		FeatureFlag flag = new FeatureFlag();
		flag.setProjectId(projectId);
		flag.setKey(input.key());
		flag.setName(input.name());
		flag.setDescription(input.description());
		flag.setFlagType(input.flagType());
		flag.setVariations(input.variations());
		flag.setTags(input.tags());
		flag.setTemporary(input.temporary());
		flag.setCreatedBy(actor == null ? null : actor.getId());

		FeatureFlag created = flagRepository.create(flag);
		initializeConfigs(created);

		historyService.record(projectId, created.getId(), null, null, "FLAG_CREATED",
				actor.getName() + " created flag \"" + created.getName() + "\".", null, created.toMap(), actor);

		return created;
	}

	public FeatureFlag update(String flagId, Map<String, Object> fields, Account actor) {
		FeatureFlag before = requireFlag(flagId);
		FeatureFlag after = flagRepository.update(flagId, fields);

		historyService.record(after.getProjectId(), flagId, null, null, "FLAG_UPDATED",
				actor.getName() + " updated flag details.", before.toMap(), after.toMap(), actor);

		return after;
	}

	public FeatureFlag setArchived(String flagId, boolean archived, Account actor) {
		FeatureFlag before = requireFlag(flagId);
		FeatureFlag after = flagRepository.setArchived(flagId, archived);

		historyService.record(after.getProjectId(), flagId, null, null, archived ? "FLAG_ARCHIVED" : "FLAG_RESTORED",
				actor.getName() + (archived ? " archived this flag." : " restored this flag."), before.toMap(),
				after.toMap(), actor);

		return after;
	}

	public FeatureFlag requireFlag(String flagId) {
		if (!ObjectIds.isValid(flagId)) {
			throw notFound();
		}

		FeatureFlag flag = flagRepository.findById(flagId);

		if (flag == null) {
			throw notFound();
		}

		return flag;
	}

	/** Seeds a disabled default configuration for every environment already in the project. */
	private void initializeConfigs(FeatureFlag flag) {
		List<Environment> environments = environmentService.listByProject(flag.getProjectId());
		String defaultVariationId = flag.getVariations().isEmpty() ? null : flag.getVariations().get(0).getId();

		for (Environment environment : environments) {
			FlagEnvironmentConfig config = new FlagEnvironmentConfig();
			config.setFlagId(flag.getId());
			config.setEnvironmentId(environment.getId());
			config.setEnvironmentKey(environment.getKey());
			config.setEnabled(false);
			config.setOffVariationId(defaultVariationId);
			config.setFallthroughVariationId(defaultVariationId);
			flagConfigRepository.save(config);
		}
	}

	private ApiException notFound() {
		return new ApiException(NOT_FOUND, "FLAG_NOT_FOUND", "The requested flag does not exist.");
	}
}
