package com.featureflags.audiences;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.projects.ProjectService;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.ObjectIds;

@Service
public class AppUserService {

	private static final int NOT_FOUND = 404;

	private static final int CONFLICT = 409;

	private final AppUserRepository appUserRepository;

	private final ProjectService projectService;

	public AppUserService(AppUserRepository appUserRepository, ProjectService projectService) {
		this.appUserRepository = appUserRepository;
		this.projectService = projectService;
	}

	public List<AppUser> list(String projectId, String search) {
		projectService.requireProject(projectId);

		return appUserRepository.listByProject(projectId, search);
	}

	public AppUser create(String projectId, AppUserValidator.CreateInput input) {
		projectService.requireProject(projectId);

		if (appUserRepository.findByKey(projectId, input.key()) != null) {
			throw new ApiException(CONFLICT, "USER_KEY_CONFLICT", "A user with this key already exists.");
		}

		AppUser user = new AppUser();
		user.setProjectId(projectId);
		user.setKey(input.key());
		user.setName(input.name());
		user.setEmail(input.email());
		user.setPlan(input.plan());
		user.setCountry(input.country());
		user.setAttributes(input.attributes());

		return appUserRepository.create(user);
	}

	public AppUser update(String id, Map<String, Object> fields) {
		require(id);

		return appUserRepository.update(id, fields);
	}

	public void remove(String id) {
		require(id);
		appUserRepository.remove(id);
	}

	private AppUser require(String id) {
		if (!ObjectIds.isValid(id)) {
			throw notFound();
		}

		AppUser user = appUserRepository.findById(id);

		if (user == null) {
			throw notFound();
		}

		return user;
	}

	private ApiException notFound() {
		return new ApiException(NOT_FOUND, "USER_NOT_FOUND", "The requested user does not exist.");
	}
}
