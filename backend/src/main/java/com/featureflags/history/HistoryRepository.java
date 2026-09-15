package com.featureflags.history;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class HistoryRepository {

	private static final int DEFAULT_LIMIT = 100;

	private final MongoTemplate mongoTemplate;

	public HistoryRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public AuditLogEntry insert(AuditLogEntry entry) {
		return mongoTemplate.insert(entry);
	}

	public List<AuditLogEntry> findByFlagId(String flagId) {
		Query query = Query.query(Criteria.where("flagId").is(new ObjectId(flagId)))
				.with(Sort.by(Sort.Order.desc("createdAt"))).limit(DEFAULT_LIMIT);

		return mongoTemplate.find(query, AuditLogEntry.class);
	}

	public List<AuditLogEntry> findByProjectId(String projectId, int limit) {
		Query query = Query.query(Criteria.where("projectId").is(new ObjectId(projectId)))
				.with(Sort.by(Sort.Order.desc("createdAt"))).limit(limit);

		return mongoTemplate.find(query, AuditLogEntry.class);
	}

	public AuditLogEntry findById(String id) {
		if (!ObjectId.isValid(id)) {
			return null;
		}

		return mongoTemplate.findById(id, AuditLogEntry.class);
	}
}
