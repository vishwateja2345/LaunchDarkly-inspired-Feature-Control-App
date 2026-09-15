package com.featureflags.auth;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

	private final AuthRepository authRepository;

	public AccountController(AuthRepository authRepository) {
		this.authRepository = authRepository;
	}

	@GetMapping
	public Map<String, Object> list() {
		List<Map<String, Object>> accounts = authRepository.listActive().stream().map(Account::toPublicMap).toList();

		return Map.of("data", accounts);
	}
}
