package com.featureflags.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

@Service
public class JwtService {

	private static final String ISSUER = "featureflags-api";

	private static final String AUDIENCE = "featureflags-app";

	private static final int SECONDS_PER_HOUR = 3600;

	private static final int SECONDS_PER_MINUTE = 60;

	private static final int SECONDS_PER_DAY = 86400;

	public record Payload(String subject) {
	}

	private final Algorithm algorithm;

	private final JWTVerifier verifier;

	private final Duration expiresIn;

	public JwtService(@Value("${featureflags.jwt.secret}") String secret,
			@Value("${featureflags.jwt.expires-in}") String expiresIn) {
		this.algorithm = Algorithm.HMAC256(secret);
		this.verifier = JWT.require(algorithm).withIssuer(ISSUER).withAudience(AUDIENCE).build();
		this.expiresIn = parseDuration(expiresIn);
	}

	public String issue(String accountId) {
		Instant now = Instant.now();

		return JWT.create().withSubject(accountId).withIssuedAt(Date.from(now))
				.withExpiresAt(Date.from(now.plus(expiresIn))).withIssuer(ISSUER).withAudience(AUDIENCE)
				.sign(algorithm);
	}

	public Payload read(String token) {
		DecodedJWT decoded = verifier.verify(token);

		return new Payload(decoded.getSubject());
	}

	private static Duration parseDuration(String value) {
		String trimmed = value.trim();
		char unit = trimmed.charAt(trimmed.length() - 1);

		if (!Character.isLetter(unit)) {
			return Duration.ofSeconds(Long.parseLong(trimmed));
		}

		long amount = Long.parseLong(trimmed.substring(0, trimmed.length() - 1));

		return Duration.ofSeconds(amount * secondsFor(unit));
	}

	private static int secondsFor(char unit) {
		return switch (unit) {
			case 'd' -> SECONDS_PER_DAY;
			case 'h' -> SECONDS_PER_HOUR;
			case 'm' -> SECONDS_PER_MINUTE;
			default -> 1;
		};
	}
}
