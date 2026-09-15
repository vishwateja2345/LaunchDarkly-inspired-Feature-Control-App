package com.featureflags.flags;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class FlagConfigRepository {

	private final MongoTemplate mongoTemplate;

	public FlagConfigRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public FlagEnvironmentConfig find(String flagId, String environmentId) {
		Criteria criteria = Criteria.where("flagId").is(new ObjectId(flagId)).and("environmentId")
				.is(new ObjectId(environmentId));

		return mongoTemplate.findOne(Query.query(criteria), FlagEnvironmentConfig.class);
	}

	public List<FlagEnvironmentConfig> findAllForFlag(String flagId) {
		return mongoTemplate.find(Query.query(Criteria.where("flagId").is(new ObjectId(flagId))),
				FlagEnvironmentConfig.class);
	}

	public FlagEnvironmentConfig save(FlagEnvironmentConfig config) {
		return mongoTemplate.save(config);
	}

	public void removeAllForFlag(String flagId) {
		mongoTemplate.remove(Query.query(Criteria.where("flagId").is(new ObjectId(flagId))),
				FlagEnvironmentConfig.class);
	}
}
