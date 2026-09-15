package com.featureflags.environments;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class EnvironmentRepository {

	private final MongoTemplate mongoTemplate;

	public EnvironmentRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Environment> listByProject(String projectId) {
		Query query = Query.query(Criteria.where("projectId").is(new ObjectId(projectId)))
				.with(Sort.by(Sort.Order.asc("sortOrder")));

		return mongoTemplate.find(query, Environment.class);
	}

	public Environment findById(String id) {
		if (!ObjectId.isValid(id)) {
			return null;
		}

		return mongoTemplate.findById(id, Environment.class);
	}

	public Environment findByKey(String projectId, String key) {
		Criteria criteria = Criteria.where("projectId").is(new ObjectId(projectId)).and("key")
				.regex(Pattern.compile("^" + Pattern.quote(key) + "$", Pattern.CASE_INSENSITIVE));

		return mongoTemplate.findOne(Query.query(criteria), Environment.class);
	}

	public long countByProject(String projectId) {
		return mongoTemplate.count(Query.query(Criteria.where("projectId").is(new ObjectId(projectId))),
				Environment.class);
	}

	public Environment create(Environment environment) {
		return mongoTemplate.insert(environment);
	}

	public Environment update(String id, Map<String, Object> fields) {
		Update update = new Update();
		fields.forEach(update::set);
		mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(id))), update, Environment.class);

		return findById(id);
	}

	public void remove(String id) {
		mongoTemplate.remove(Query.query(Criteria.where("_id").is(new ObjectId(id))), Environment.class);
	}
}
