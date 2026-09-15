package com.featureflags.auth;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.featureflags.shared.RequestContext;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	private final RequestContext requestContext;

	public AuthController(AuthService authService, RequestContext requestContext) {
		this.authService = authService;
		this.requestContext = requestContext;
	}

	@PostMapping("/login")
	public Map<String, Object> login(@RequestBody(required = false) Map<String, Object> body) {
		AuthValidator.Credentials credentials = AuthValidator.validateLogin(body);

		return Map.of("data", authService.login(credentials.email(), credentials.password()));
	}

	@GetMapping("/session")
	public Map<String, Object> session() {
		return Map.of("data", authService.session(requestContext.account()));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout() {
		// Stateless JWTs: the client discards its token. Nothing to invalidate server-side.
	}
}
