package com.featureflags.approvals;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ApprovalRepository {

	private final MongoTemplate mongoTemplate;

	public ApprovalRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public ApprovalRequest findById(String id) {
		if (!ObjectId.isValid(id)) {
			return null;
		}

		return mongoTemplate.findById(id, ApprovalRequest.class);
	}

	public List<ApprovalRequest> listByProject(String projectId, String status) {
		Criteria criteria = Criteria.where("projectId").is(new ObjectId(projectId));

		if (status != null && !status.isBlank()) {
			criteria = criteria.and("status").is(status);
		}

		Query query = Query.query(criteria).with(Sort.by(Sort.Order.desc("createdAt")));

		return mongoTemplate.find(query, ApprovalRequest.class);
	}

	public List<ApprovalRequest> findDueScheduled(java.time.Instant now) {
		Criteria criteria = Criteria.where("status").is(ApprovalRequest.STATUS_SCHEDULED).and("scheduledFor")
				.lte(now);

		return mongoTemplate.find(Query.query(criteria), ApprovalRequest.class);
	}

	public ApprovalRequest save(ApprovalRequest request) {
		return mongoTemplate.save(request);
	}
}
