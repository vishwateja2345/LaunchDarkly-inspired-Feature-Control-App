package com.featureflags.auth;

import java.io.IOException;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.featureflags.shared.ApiException;
import com.featureflags.shared.GlobalExceptionHandler;
import com.featureflags.shared.RequestContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiAuthFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private static final String API_PREFIX = "/api/v1";

	private static final List<String> PUBLIC_GET = List.of("/api/v1/health");

	private static final List<String> PUBLIC_POST = List.of("/api/v1/auth/login");

	private static final int UNAUTHORIZED = 401;

	private final AuthService authService;

	private final ObjectMapper objectMapper;

	public ApiAuthFilter(AuthService authService, ObjectMapper objectMapper) {
		this.authService = authService;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();

		return !path.startsWith(API_PREFIX) || isPublic(request, path);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		try {
			Account account = authService.authenticate(readToken(request));
			request.setAttribute(RequestContext.ACCOUNT, account);
		} catch (ApiException exception) {
			write(response, exception);

			return;
		}

		chain.doFilter(request, response);
	}

	private boolean isPublic(HttpServletRequest request, String path) {
		return "GET".equals(request.getMethod()) && PUBLIC_GET.contains(path)
				|| "POST".equals(request.getMethod()) && PUBLIC_POST.contains(path);
	}

	private String readToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");

		if (header == null || !header.startsWith(BEARER_PREFIX)) {
			throw authRequired();
		}

		String token = header.substring(BEARER_PREFIX.length()).trim();

		if (token.isEmpty()) {
			throw authRequired();
		}

		return token;
	}

	private ApiException authRequired() {
		return new ApiException(UNAUTHORIZED, "AUTH_REQUIRED", "Sign in to continue.");
	}

	private void write(HttpServletResponse response, ApiException exception) throws IOException {
		response.setStatus(exception.getStatusCode());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(),
				GlobalExceptionHandler.envelope(exception.getCode(), exception.getMessage(), exception.getDetails()));
	}
}
