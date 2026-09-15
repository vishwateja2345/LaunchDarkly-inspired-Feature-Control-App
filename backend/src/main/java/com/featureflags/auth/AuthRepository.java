package com.featureflags.auth;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {

	private final MongoTemplate mongoTemplate;

	public AuthRepository(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public Account findActiveByEmail(String email) {
		return mongoTemplate.findOne(Query.query(Criteria.where("email").is(email).and("active").is(true)),
				Account.class);
	}

	public Account findActiveById(String id) {
		if (!ObjectId.isValid(id)) {
			return null;
		}

		return mongoTemplate.findOne(
				Query.query(Criteria.where("_id").is(new ObjectId(id)).and("active").is(true)), Account.class);
	}

	public Account findById(String id) {
		if (!ObjectId.isValid(id)) {
			return null;
		}

		return mongoTemplate.findById(id, Account.class);
	}

	public List<Account> listActive() {
		return mongoTemplate.find(Query.query(Criteria.where("active").is(true)).with(Sort.by(Sort.Order.asc("name"))),
				Account.class);
	}
}
