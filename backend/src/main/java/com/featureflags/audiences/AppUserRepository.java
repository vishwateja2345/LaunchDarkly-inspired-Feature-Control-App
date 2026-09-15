package com.featureflags.audiences;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class AppUserRepository {

	private final MongoTemplate mongoTemplate;

	public AppUserRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<AppUser> listByProject(String projectId, String search) {
		Criteria criteria = Criteria.where("projectId").is(new ObjectId(projectId));

		if (search != null && !search.isBlank()) {
			criteria = criteria.orOperator(Criteria.where("key").regex(search, "i"),
					Criteria.where("name").regex(search, "i"), Criteria.where("email").regex(search, "i"));
			criteria = new Criteria().andOperator(Criteria.where("projectId").is(new ObjectId(projectId)), criteria);
		}

		Query query = Query.query(criteria).with(Sort.by(Sort.Order.asc("name"))).limit(500);

		return mongoTemplate.find(query, AppUser.class);
	}

	public AppUser findById(String id) {
		if (!ObjectId.isValid(id)) {
			return null;
		}

		return mongoTemplate.findById(id, AppUser.class);
	}

	public AppUser findByKey(String projectId, String key) {
		return mongoTemplate.findOne(
				Query.query(Criteria.where("projectId").is(new ObjectId(projectId)).and("key").is(key)),
				AppUser.class);
	}

	public long countByProject(String projectId) {
		return mongoTemplate.count(Query.query(Criteria.where("projectId").is(new ObjectId(projectId))),
				AppUser.class);
	}

	public AppUser create(AppUser user) {
		return mongoTemplate.insert(user);
	}

	public AppUser update(String id, java.util.Map<String, Object> fields) {
		Update update = new Update();
		fields.forEach(update::set);
		mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(id))), update, AppUser.class);

		return findById(id);
	}

	public void remove(String id) {
		mongoTemplate.remove(Query.query(Criteria.where("_id").is(new ObjectId(id))), AppUser.class);
	}
}
