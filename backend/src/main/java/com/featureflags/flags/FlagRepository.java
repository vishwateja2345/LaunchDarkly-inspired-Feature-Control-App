package com.featureflags.flags;

import java.time.Instant;
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
public class FlagRepository {

	private final MongoTemplate mongoTemplate;

	public FlagRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<FeatureFlag> listByProject(String projectId, boolean includeArchived) {
		Criteria criteria = Criteria.where("projectId").is(new ObjectId(projectId));

		if (!includeArchived) {
			criteria = criteria.and("archived").is(false);
		}

		return mongoTemplate.find(Query.query(criteria).with(Sort.by(Sort.Order.asc("name"))), FeatureFlag.class);
	}

	public FeatureFlag findById(String id) {
		if (!ObjectId.isValid(id)) {
			return null;
		}

		return mongoTemplate.findById(id, FeatureFlag.class);
	}

	public FeatureFlag findByKey(String projectId, String key) {
		Criteria criteria = Criteria.where("projectId").is(new ObjectId(projectId)).and("key")
				.regex(Pattern.compile("^" + Pattern.quote(key) + "$", Pattern.CASE_INSENSITIVE));

		return mongoTemplate.findOne(Query.query(criteria), FeatureFlag.class);
	}

	public FeatureFlag create(FeatureFlag flag) {
		return mongoTemplate.insert(flag);
	}

	public FeatureFlag update(String id, Map<String, Object> fields) {
		Update update = new Update();
		fields.forEach(update::set);
		update.set("updatedAt", Instant.now());
		mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(id))), update, FeatureFlag.class);

		return findById(id);
	}

	public FeatureFlag setArchived(String id, boolean archived) {
		Update update = new Update().set("archived", archived).set("archivedAt", archived ? Instant.now() : null)
				.set("updatedAt", Instant.now());
		mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(id))), update, FeatureFlag.class);

		return findById(id);
	}
}
