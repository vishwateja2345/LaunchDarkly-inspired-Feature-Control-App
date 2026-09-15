package com.featureflags.projects;

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
public class ProjectRepository {

	private final MongoTemplate mongoTemplate;

	public ProjectRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Project> list() {
		return mongoTemplate.find(new Query().with(Sort.by(Sort.Order.asc("name"))), Project.class);
	}

	public Project findById(String id) {
		if (!ObjectId.isValid(id)) {
			return null;
		}

		return mongoTemplate.findById(id, Project.class);
	}

	public Project findByKey(String key, String excludedId) {
		Criteria criteria = Criteria.where("key")
				.regex(Pattern.compile("^" + Pattern.quote(key) + "$", Pattern.CASE_INSENSITIVE));

		if (excludedId != null) {
			criteria = criteria.and("_id").ne(new ObjectId(excludedId));
		}

		return mongoTemplate.findOne(Query.query(criteria), Project.class);
	}

	public Project create(Project project) {
		return mongoTemplate.insert(project);
	}

	public Project update(String id, Map<String, Object> fields) {
		Update update = new Update();
		fields.forEach(update::set);
		update.set("updatedAt", Instant.now());
		mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(id))), update, Project.class);

		return findById(id);
	}

	public void remove(String id) {
		mongoTemplate.remove(Query.query(Criteria.where("_id").is(new ObjectId(id))), Project.class);
	}
}
