package com.featureflags.auth;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.featureflags.shared.ApiException;
import com.featureflags.shared.ObjectIds;

import at.favre.lib.crypto.bcrypt.BCrypt;

@Service
public class AuthService {

	private static final int UNAUTHORIZED = 401;

	private static final String INVALID_TOKEN_MESSAGE = "Your session is invalid or has expired.";

	private final AuthRepository authRepository;

	private final JwtService jwtService;

	public AuthService(AuthRepository authRepository, JwtService jwtService) {
		this.authRepository = authRepository;
		this.jwtService = jwtService;
	}

	public Map<String, Object> login(String email, String password) {
		Account account = authRepository.findActiveByEmail(email.toLowerCase());
		boolean valid = account != null
				&& BCrypt.verifyer().verify(password.toCharArray(), account.getPasswordHash()).verified;

		if (!valid) {
			throw new ApiException(UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect.");
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("account", account.toPublicMap());
		body.put("token", jwtService.issue(account.getId()));

		return body;
	}

	public Account authenticate(String token) {
		JwtService.Payload payload = readPayload(token);

		if (!ObjectIds.isValid(payload.subject())) {
			throw new ApiException(UNAUTHORIZED, "INVALID_TOKEN", INVALID_TOKEN_MESSAGE);
		}

		Account account = authRepository.findActiveById(payload.subject());

		if (account == null) {
			throw new ApiException(UNAUTHORIZED, "ACCOUNT_UNAVAILABLE", "This account is no longer available.");
		}

		return account;
	}

	public Map<String, Object> session(Account account) {
		return Map.of("account", account.toPublicMap());
	}

	private JwtService.Payload readPayload(String token) {
		try {
			return jwtService.read(token);
		} catch (RuntimeException exception) {
			throw new ApiException(UNAUTHORIZED, "INVALID_TOKEN", INVALID_TOKEN_MESSAGE);
		}
	}
}
