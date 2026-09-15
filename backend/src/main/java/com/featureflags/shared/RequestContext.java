package com.featureflags.shared;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.featureflags.auth.Account;

@Component
public class RequestContext {

	public static final String ACCOUNT = "featureflags.account";

	public Account account() {
		return (Account) attribute(ACCOUNT);
	}

	public String accountId() {
		Account account = account();

		return account == null ? null : account.getId();
	}

	private Object attribute(String name) {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

		return attributes == null ? null : attributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
	}
}
