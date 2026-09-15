package com.featureflags.audiences;

import java.util.List;

import org.springframework.stereotype.Service;

import com.featureflags.projects.ProjectService;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.ObjectIds;

@Service
public class SegmentService {

	private static final int NOT_FOUND = 404;

	private static final int CONFLICT = 409;

	private final SegmentRepository segmentRepository;

	private final AppUserRepository appUserRepository;

	private final ProjectService projectService;

	public SegmentService(SegmentRepository segmentRepository, AppUserRepository appUserRepository,
			ProjectService projectService) {
		this.segmentRepository = segmentRepository;
		this.appUserRepository = appUserRepository;
		this.projectService = projectService;
	}

	public List<Segment> list(String projectId) {
		projectService.requireProject(projectId);

		return segmentRepository.listByProject(projectId);
	}

	public Segment get(String segmentId) {
		return require(segmentId);
	}

	public Segment create(String projectId, SegmentValidator.Input input) {
		projectService.requireProject(projectId);

		if (segmentRepository.findByKey(projectId, input.key()) != null) {
			throw new ApiException(CONFLICT, "SEGMENT_KEY_CONFLICT", "A segment with this key already exists.");
		}

		Segment segment = new Segment();
		segment.setProjectId(projectId);
		segment.setKey(input.key());
		segment.setName(input.name());
		segment.setDescription(input.description());
		segment.setRules(input.rules());
		segment.setIncludedKeys(input.includedKeys());
		segment.setExcludedKeys(input.excludedKeys());

		return segmentRepository.create(segment);
	}

	public Segment update(String segmentId, SegmentValidator.Input input) {
		Segment segment = require(segmentId);
		segment.setName(input.name());
		segment.setDescription(input.description());
		segment.setRules(input.rules());
		segment.setIncludedKeys(input.includedKeys());
		segment.setExcludedKeys(input.excludedKeys());

		return segmentRepository.update(segmentId, segment);
	}

	public void remove(String segmentId) {
		require(segmentId);
		segmentRepository.remove(segmentId);
	}

	/** Counts how many of the project's known app users currently match this segment. */
	public int previewMatchCount(String segmentId) {
		Segment segment = require(segmentId);
		List<AppUser> users = appUserRepository.listByProject(segment.getProjectId(), null);

		return (int) users.stream().filter(user -> SegmentMatcher.matches(segment, user.toContext())).count();
	}

	private Segment require(String segmentId) {
		if (!ObjectIds.isValid(segmentId)) {
			throw notFound();
		}

		Segment segment = segmentRepository.findById(segmentId);

		if (segment == null) {
			throw notFound();
		}

		return segment;
	}

	private ApiException notFound() {
		return new ApiException(NOT_FOUND, "SEGMENT_NOT_FOUND", "The requested segment does not exist.");
	}
}
