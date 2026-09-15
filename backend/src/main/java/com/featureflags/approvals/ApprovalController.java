package com.featureflags.approvals;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.featureflags.environments.Environment;
import com.featureflags.environments.EnvironmentService;
import com.featureflags.flags.FeatureFlag;
import com.featureflags.flags.FlagService;
import com.featureflags.shared.RequestContext;

@RestController
public class ApprovalController {

	private final ApprovalService approvalService;

	private final FlagService flagService;

	private final EnvironmentService environmentService;

	private final RequestContext requestContext;

	public ApprovalController(ApprovalService approvalService, FlagService flagService,
			EnvironmentService environmentService, RequestContext requestContext) {
		this.approvalService = approvalService;
		this.flagService = flagService;
		this.environmentService = environmentService;
		this.requestContext = requestContext;
	}

	@GetMapping("/api/v1/approvals")
	public Map<String, Object> list(@RequestParam String projectId, @RequestParam(required = false) String status) {
		return Map.of("data", approvalService.list(projectId, status).stream().map(ApprovalRequest::toMap).toList());
	}

	@GetMapping("/api/v1/approvals/{approvalId}")
	public Map<String, Object> get(@PathVariable String approvalId) {
		return Map.of("data", approvalService.get(approvalId).toMap());
	}

	@PostMapping("/api/v1/approvals/{approvalId}/approve")
	public Map<String, Object> approve(@PathVariable String approvalId,
			@RequestBody(required = false) Map<String, Object> body) {
		ApprovalRequest request = approvalService.get(approvalId);
		FeatureFlag flag = flagService.get(request.getFlagId());
		Environment environment = environmentService.requireEnvironment(request.getEnvironmentId());
		ApprovalValidator.ReviewInput input = ApprovalValidator.validateApprove(body);

		ApprovalRequest updated = approvalService.approve(approvalId, flag, environment, input.comment(),
				input.scheduleFor(), requestContext.account());

		return Map.of("data", updated.toMap());
	}

	@PostMapping("/api/v1/approvals/{approvalId}/reject")
	public Map<String, Object> reject(@PathVariable String approvalId,
			@RequestBody(required = false) Map<String, Object> body) {
		ApprovalValidator.RejectInput input = ApprovalValidator.validateReject(body);
		ApprovalRequest updated = approvalService.reject(approvalId, requestContext.account(), input.comment());

		return Map.of("data", updated.toMap());
	}

	@PostMapping("/api/v1/approvals/{approvalId}/cancel")
	public Map<String, Object> cancel(@PathVariable String approvalId) {
		return Map.of("data", approvalService.cancel(approvalId, requestContext.account()).toMap());
	}
}
