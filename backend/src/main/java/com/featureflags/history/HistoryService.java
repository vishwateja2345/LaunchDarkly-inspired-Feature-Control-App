package com.featureflags.history;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.auth.Account;
import com.featureflags.shared.ApiException;
import com.featureflags.shared.ObjectIds;

/**
 * Records and retrieves the audit trail. This service intentionally accepts plain
 * identifiers and snapshot maps rather than domain objects so it has no dependency on
 * the flags/approvals features that write to it - any feature can log an entry.
 */
@Service
public class HistoryService {

	private static final int NOT_FOUND = 404;

	private final HistoryRepository historyRepository;

	public HistoryService(HistoryRepository historyRepository) {
		this.historyRepository = historyRepository;
	}

	public AuditLogEntry record(String projectId, String flagId, String environmentId, String environmentKey,
			String action, String summary, Map<String, Object> before, Map<String, Object> after, Account actor) {
		AuditLogEntry entry = new AuditLogEntry();
		entry.setProjectId(projectId);
		entry.setFlagId(flagId);
		entry.setEnvironmentId(environmentId);
		entry.setEnvironmentKey(environmentKey);
		entry.setAction(action);
		entry.setSummary(summary);
		entry.setBeforeSnapshot(before);
		entry.setAfterSnapshot(after);
		entry.setActorId(actor == null ? null : actor.getId());
		entry.setActorName(actor == null ? "System" : actor.getName());

		return historyRepository.insert(entry);
	}

	public List<AuditLogEntry> listForFlag(String flagId) {
		if (!ObjectIds.isValid(flagId)) {
			throw notFound();
		}

		return historyRepository.findByFlagId(flagId);
	}

	public List<AuditLogEntry> listForProject(String projectId, int limit) {
		return historyRepository.findByProjectId(projectId, limit);
	}

	public AuditLogEntry get(String entryId) {
		AuditLogEntry entry = historyRepository.findById(entryId);

		if (entry == null) {
			throw notFound();
		}

		return entry;
	}

	private ApiException notFound() {
		return new ApiException(NOT_FOUND, "HISTORY_ENTRY_NOT_FOUND", "The requested history entry does not exist.");
	}
}
