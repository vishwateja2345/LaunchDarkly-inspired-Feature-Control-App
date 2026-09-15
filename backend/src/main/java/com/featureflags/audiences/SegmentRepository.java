package com.featureflags.audiences;

import java.util.List;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class SegmentRepository {

	private final MongoTemplate mongoTemplate;

	public SegmentRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Segment> listByProject(String projectId) {
		Query query = Query.query(Criteria.where("projectId").is(new ObjectId(projectId)))
				.with(Sort.by(Sort.Order.asc("name")));

		return mongoTemplate.find(query, Segment.class);
	}

	public Segment findById(String id) {
		if (!ObjectId.isValid(id)) {
			return null;
		}

		return mongoTemplate.findById(id, Segment.class);
	}

	public Segment findByKey(String projectId, String key) {
		Criteria criteria = Criteria.where("projectId").is(new ObjectId(projectId)).and("key")
				.regex(Pattern.compile("^" + Pattern.quote(key) + "$", Pattern.CASE_INSENSITIVE));

		return mongoTemplate.findOne(Query.query(criteria), Segment.class);
	}

	public Segment create(Segment segment) {
		return mongoTemplate.insert(segment);
	}

	public Segment update(String id, Segment segment) {
		Update update = new Update().set("name", segment.getName()).set("description", segment.getDescription())
				.set("rules", segment.getRules()).set("includedKeys", segment.getIncludedKeys())
				.set("excludedKeys", segment.getExcludedKeys());
		mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(new ObjectId(id))), update, Segment.class);

		return findById(id);
	}

	public void remove(String id) {
		mongoTemplate.remove(Query.query(Criteria.where("_id").is(new ObjectId(id))), Segment.class);
	}
}
