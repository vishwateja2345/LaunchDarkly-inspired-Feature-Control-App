package com.featureflags.approvals;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.auth.Account;
import com.featureflags.environments.Environment;
import com.featureflags.flags.FeatureFlag;
import com.featureflags.flags.FlagConfigService;
import com.featureflags.flags.FlagConfigValidator;
import com.featureflags.history.HistoryService;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.ObjectIds;

@Service
public class ApprovalService {

	private static final int NOT_FOUND = 404;

	private static final int CONFLICT = 409;

	private static final int FORBIDDEN = 403;

	private final ApprovalRepository approvalRepository;

	private final FlagConfigService flagConfigService;

	private final HistoryService historyService;

	public ApprovalService(ApprovalRepository approvalRepository, FlagConfigService flagConfigService,
			HistoryService historyService) {
		this.approvalRepository = approvalRepository;
		this.flagConfigService = flagConfigService;
		this.historyService = historyService;
	}

	public List<ApprovalRequest> list(String projectId, String status) {
		return approvalRepository.listByProject(projectId, status);
	}

	public ApprovalRequest get(String approvalId) {
		return require(approvalId);
	}

	/** Validates the proposed change up front so a request can never be created unapprovable. */
	public ApprovalRequest propose(FeatureFlag flag, Environment environment, Map<String, Object> rawChange,
			String reason, Account requester) {
		FlagConfigValidator.validate(flag, rawChange);

		ApprovalRequest request = new ApprovalRequest();
		request.setProjectId(flag.getProjectId());
		request.setFlagId(flag.getId());
		request.setFlagName(flag.getName());
		request.setEnvironmentId(environment.getId());
		request.setEnvironmentKey(environment.getKey());
		request.setEnvironmentName(environment.getName());
		request.setRequestedBy(requester.getId());
		request.setRequestedByName(requester.getName());
		request.setProposedChange(new LinkedHashMap<>(rawChange));
		request.setReason(reason == null ? "" : reason);
		request.setStatus(ApprovalRequest.STATUS_PENDING);

		ApprovalRequest saved = approvalRepository.save(request);

		historyService.record(flag.getProjectId(), flag.getId(), environment.getId(), environment.getKey(),
				"CHANGE_PROPOSED", requester.getName() + " proposed a change to " + environment.getName()
						+ " that requires approval before it goes live.",
				null, null, requester);

		return saved;
	}

	public ApprovalRequest approve(String approvalId, FeatureFlag flag, Environment environment, String comment,
			Instant scheduleFor, Account reviewer) {
		ApprovalRequest request = requirePending(approvalId);
		ensureNotSelfReview(request, reviewer);

		request.setReviewerId(reviewer.getId());
		request.setReviewerName(reviewer.getName());
		request.setReviewComment(comment);
		request.setReviewedAt(Instant.now());

		if (scheduleFor != null) {
			request.setStatus(ApprovalRequest.STATUS_SCHEDULED);
			request.setScheduledFor(scheduleFor);
			ApprovalRequest saved = approvalRepository.save(request);

			historyService.record(flag.getProjectId(), flag.getId(), environment.getId(), environment.getKey(),
					"CHANGE_SCHEDULED",
					reviewer.getName() + " approved and scheduled this change for " + scheduleFor + ".", null, null,
					reviewer);

			return saved;
		}

		applyApprovedChange(request, flag, environment, reviewer);
		request.setStatus(ApprovalRequest.STATUS_APPLIED);

		return approvalRepository.save(request);
	}

	public ApprovalRequest reject(String approvalId, Account reviewer, String comment) {
		ApprovalRequest request = requirePending(approvalId);
		ensureNotSelfReview(request, reviewer);

		request.setStatus(ApprovalRequest.STATUS_REJECTED);
		request.setReviewerId(reviewer.getId());
		request.setReviewerName(reviewer.getName());
		request.setReviewComment(comment);
		request.setReviewedAt(Instant.now());

		ApprovalRequest saved = approvalRepository.save(request);

		historyService.record(request.getProjectId(), request.getFlagId(), request.getEnvironmentId(),
				request.getEnvironmentKey(), "CHANGE_REJECTED",
				reviewer.getName() + " rejected this change" + (comment.isBlank() ? "." : ": " + comment), null, null,
				reviewer);

		return saved;
	}

	public ApprovalRequest cancel(String approvalId, Account requester) {
		ApprovalRequest request = require(approvalId);

		if (!isPendingOrScheduled(request)) {
			throw new ApiException(CONFLICT, "APPROVAL_NOT_CANCELLABLE", "This request is no longer pending.");
		}

		if (!request.getRequestedBy().equals(requester.getId())) {
			throw new ApiException(FORBIDDEN, "NOT_REQUESTER", "Only the requester can cancel this change.");
		}

		request.setStatus(ApprovalRequest.STATUS_CANCELLED);
		ApprovalRequest saved = approvalRepository.save(request);

		historyService.record(request.getProjectId(), request.getFlagId(), request.getEnvironmentId(),
				request.getEnvironmentKey(), "CHANGE_CANCELLED", requester.getName() + " cancelled this request.",
				null, null, requester);

		return saved;
	}

	/** Called by {@link ApprovalScheduler} for requests whose scheduled time has arrived. */
	public void applyScheduled(ApprovalRequest request, FeatureFlag flag, Environment environment) {
		applyApprovedChange(request, flag, environment, null);
		request.setStatus(ApprovalRequest.STATUS_APPLIED);
		approvalRepository.save(request);
	}

	public List<ApprovalRequest> findDueScheduled() {
		return approvalRepository.findDueScheduled(Instant.now());
	}

	private void applyApprovedChange(ApprovalRequest request, FeatureFlag flag, Environment environment,
			Account reviewer) {
		FlagConfigValidator.ParsedChange change = FlagConfigValidator.validate(flag, request.getProposedChange());
		String actorName = reviewer == null ? "Scheduled approval" : reviewer.getName();
		flagConfigService.applyDirect(flag, environment, change, reviewer, "CONFIG_UPDATED",
				actorName + " applied the approved change (requested by " + request.getRequestedByName() + ").");
	}

	private void ensureNotSelfReview(ApprovalRequest request, Account reviewer) {
		if (request.getRequestedBy().equals(reviewer.getId())) {
			throw new ApiException(FORBIDDEN, "SELF_APPROVAL_FORBIDDEN",
					"You cannot approve or reject your own change request.");
		}
	}

	private boolean isPendingOrScheduled(ApprovalRequest request) {
		return ApprovalRequest.STATUS_PENDING.equals(request.getStatus())
				|| ApprovalRequest.STATUS_SCHEDULED.equals(request.getStatus());
	}

	private ApprovalRequest requirePending(String approvalId) {
		ApprovalRequest request = require(approvalId);

		if (!isPendingOrScheduled(request)) {
			throw new ApiException(CONFLICT, "APPROVAL_NOT_PENDING", "This request has already been resolved.");
		}

		return request;
	}

	private ApprovalRequest require(String approvalId) {
		if (!ObjectIds.isValid(approvalId)) {
			throw notFound();
		}

		ApprovalRequest request = approvalRepository.findById(approvalId);

		if (request == null) {
			throw notFound();
		}

		return request;
	}

	private ApiException notFound() {
		return new ApiException(NOT_FOUND, "APPROVAL_NOT_FOUND", "The requested change request does not exist.");
	}
}
