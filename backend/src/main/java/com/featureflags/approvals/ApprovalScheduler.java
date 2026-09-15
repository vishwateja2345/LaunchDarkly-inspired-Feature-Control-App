package com.featureflags.approvals;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.flags.FeatureFlag;
import com.featureflags.flags.FlagService;

/** Applies scheduled flag changes once their scheduled time arrives. */
@Component
public class ApprovalScheduler {

	private static final Logger log = LoggerFactory.getLogger(ApprovalScheduler.class);

	private static final long POLL_INTERVAL_MS = 30_000;

	private final ApprovalService approvalService;

	private final FlagService flagService;

	private final EnvironmentService environmentService;

	public ApprovalScheduler(ApprovalService approvalService, FlagService flagService,
			EnvironmentService environmentService) {
		this.approvalService = approvalService;
		this.flagService = flagService;
		this.environmentService = environmentService;
	}

	@Scheduled(fixedDelay = POLL_INTERVAL_MS)
	public void applyDueChanges() {
		List<ApprovalRequest> due = approvalService.findDueScheduled();

		for (ApprovalRequest request : due) {
			try {
				FeatureFlag flag = flagService.get(request.getFlagId());
				Environment environment = environmentService.requireEnvironment(request.getEnvironmentId());
				approvalService.applyScheduled(request, flag, environment);
			} catch (RuntimeException exception) {
				log.error("Failed to apply scheduled change {}", request.getId(), exception);
			}
		}
	}
}
