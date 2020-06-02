package ru.kpfu.itis.diploma.backend.security;

import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class HeaderOrCookieSessionIdResolver implements HttpSessionIdResolver {
    private static final String HEADER_NAME = "SESSION";
    private static final String COOKIE_NAME = "SESSION";

    private final HeaderHttpSessionIdResolver headerResolver;
    private final CookieHttpSessionIdResolver cookieResolver;

    public HeaderOrCookieSessionIdResolver() {
        this.headerResolver = new HeaderHttpSessionIdResolver(HEADER_NAME);
        this.cookieResolver = new CookieHttpSessionIdResolver();

        DefaultCookieSerializer cookieSerializer = new DefaultCookieSerializer();
        cookieSerializer.setCookieName(COOKIE_NAME);
        cookieResolver.setCookieSerializer(cookieSerializer);
    }

    @Override
    public List<String> resolveSessionIds(HttpServletRequest request) {
        final List<String> resolvedFromHeader = headerResolver.resolveSessionIds(request);
        return resolvedFromHeader.isEmpty()
                ? cookieResolver.resolveSessionIds(request)
                : resolvedFromHeader;
    }

    @Override
    public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
        headerResolver.setSessionId(request, response, sessionId);
        cookieResolver.setSessionId(request, response, sessionId);
    }

    @Override
    public void expireSession(HttpServletRequest request, HttpServletResponse response) {
        headerResolver.expireSession(request, response);
        cookieResolver.expireSession(request, response);
    }
}
