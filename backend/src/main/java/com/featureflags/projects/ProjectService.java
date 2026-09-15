package com.featureflags.projects;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.shared.ApiException;
import com.featureflags.shared.ObjectIds;

@Service
public class ProjectService {

	private static final int NOT_FOUND = 404;

	private static final int CONFLICT = 409;

	private final ProjectRepository projectRepository;

	public ProjectService(ProjectRepository projectRepository) {
		this.projectRepository = projectRepository;
	}

	public List<Project> list() {
		return projectRepository.list();
	}

	public Project get(String id) {
		return requireProject(id);
	}

	public Project create(ProjectValidator.CreateInput input) {
		ensureUniqueKey(input.key(), null);

		Project project = new Project();
		project.setName(input.name());
		project.setKey(input.key());
		project.setDescription(input.description());

		return projectRepository.create(project);
	}

	public Project update(String id, Map<String, Object> fields) {
		requireProject(id);

		return projectRepository.update(id, fields);
	}

	public void remove(String id) {
		requireProject(id);
		projectRepository.remove(id);
	}

	public Project requireProject(String id) {
		if (!ObjectIds.isValid(id)) {
			throw notFound();
		}

		Project project = projectRepository.findById(id);

		if (project == null) {
			throw notFound();
		}

		return project;
	}

	private void ensureUniqueKey(String key, String excludedId) {
		if (projectRepository.findByKey(key, excludedId) != null) {
			throw new ApiException(CONFLICT, "PROJECT_KEY_CONFLICT", "A project with this key already exists.");
		}
	}

	private ApiException notFound() {
		return new ApiException(NOT_FOUND, "PROJECT_NOT_FOUND", "The requested project does not exist.");
	}
}
