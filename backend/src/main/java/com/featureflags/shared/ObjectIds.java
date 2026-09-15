package com.featureflags.shared;

import org.bson.types.ObjectId;

public final class ObjectIds {

	public static boolean isValid(String value) {
		return value != null && ObjectId.isValid(value);
	}

	public static String next() {
		return new ObjectId().toHexString();
	}

	private ObjectIds() {
	}
}
