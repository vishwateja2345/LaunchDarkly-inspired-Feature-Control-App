package com.featureflags.environments;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.projects.ProjectService;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.ObjectIds;

@Service
public class EnvironmentService {

	private static final int NOT_FOUND = 404;

	private static final int CONFLICT = 409;

	private final EnvironmentRepository environmentRepository;

	private final ProjectService projectService;

	public EnvironmentService(EnvironmentRepository environmentRepository, ProjectService projectService) {
		this.environmentRepository = environmentRepository;
		this.projectService = projectService;
	}

	public List<Environment> listByProject(String projectId) {
		projectService.requireProject(projectId);

		return environmentRepository.listByProject(projectId);
	}

	public Environment create(String projectId, EnvironmentValidator.CreateInput input) {
		projectService.requireProject(projectId);
		ensureUniqueKey(projectId, input.key());

		Environment environment = new Environment();
		environment.setProjectId(projectId);
		environment.setName(input.name());
		environment.setKey(input.key());
		environment.setColor(input.color());
		environment.setProduction(input.production());
		environment.setSortOrder((int) environmentRepository.countByProject(projectId));

		return environmentRepository.create(environment);
	}

	public Environment update(String environmentId, Map<String, Object> fields) {
		requireEnvironment(environmentId);

		return environmentRepository.update(environmentId, fields);
	}

	public void remove(String environmentId) {
		Environment environment = requireEnvironment(environmentId);

		if (environmentRepository.countByProject(environment.getProjectId()) <= 1) {
			throw new ApiException(CONFLICT, "LAST_ENVIRONMENT", "A project must keep at least one environment.");
		}

		environmentRepository.remove(environmentId);
	}

	public Environment requireEnvironment(String environmentId) {
		if (!ObjectIds.isValid(environmentId)) {
			throw notFound();
		}

		Environment environment = environmentRepository.findById(environmentId);

		if (environment == null) {
			throw notFound();
		}

		return environment;
	}

	public Environment requireByKey(String projectId, String environmentKey) {
		Environment environment = environmentRepository.findByKey(projectId, environmentKey);

		if (environment == null) {
			throw notFound();
		}

		return environment;
	}

	private void ensureUniqueKey(String projectId, String key) {
		if (environmentRepository.findByKey(projectId, key) != null) {
			throw new ApiException(CONFLICT, "ENVIRONMENT_KEY_CONFLICT",
					"An environment with this key already exists in this project.");
		}
	}

	private ApiException notFound() {
		return new ApiException(NOT_FOUND, "ENVIRONMENT_NOT_FOUND", "The requested environment does not exist.");
	}
}
