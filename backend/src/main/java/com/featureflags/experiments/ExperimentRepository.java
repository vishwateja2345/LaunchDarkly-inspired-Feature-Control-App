package com.featureflags.experiments;

import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class ExperimentRepository {

	private final MongoTemplate mongoTemplate;

	public ExperimentRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public ExperimentEvent insert(ExperimentEvent event) {
		return mongoTemplate.insert(event);
	}

	public List<ExperimentEvent> findByFlagAndEnvironment(String flagId, String environmentId, String eventType,
			String metricKey) {
		Criteria criteria = Criteria.where("flagId").is(new ObjectId(flagId)).and("environmentId")
				.is(new ObjectId(environmentId)).and("eventType").is(eventType);

		if (metricKey != null) {
			criteria = criteria.and("metricKey").is(metricKey);
		}

		return mongoTemplate.find(Query.query(criteria), ExperimentEvent.class);
	}

	public Set<String> distinctMetricKeys(String flagId, String environmentId) {
		Criteria criteria = Criteria.where("flagId").is(new ObjectId(flagId)).and("environmentId")
				.is(new ObjectId(environmentId)).and("metricKey").ne(null);
		Query query = Query.query(criteria);

		return Set.copyOf(mongoTemplate.findDistinct(query, "metricKey", ExperimentEvent.class, String.class));
	}
}
