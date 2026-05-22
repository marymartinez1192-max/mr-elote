package com.grupocinco.mrelote.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class AuthCookieService {

    private static final Duration REFRESH_MAX_AGE = Duration.ofDays(30);

    private final AuthCookieProperties props;
    private final String cookiePath;

    public AuthCookieService(AuthCookieProperties props,
                             @Value("${server.servlet.context-path:/}") String contextPath) {
        this.props = props;
        this.cookiePath = (contextPath == null || contextPath.isBlank()) ? "/" : contextPath;
    }

    public String accessCookie(String value, long maxAgeSeconds) {
        return build(props.accessName(), value, Duration.ofSeconds(maxAgeSeconds)).toString();
    }

    public String refreshCookie(String value) {
        return build(props.refreshName(), value, REFRESH_MAX_AGE).toString();
    }

    public String clearAccessCookie() {
        return build(props.accessName(), "", Duration.ZERO).toString();
    }

    public String clearRefreshCookie() {
        return build(props.refreshName(), "", Duration.ZERO).toString();
    }

    public Optional<String> readRefresh(HttpServletRequest request) {
        return read(request, props.refreshName());
    }

    public Optional<String> readAccess(HttpServletRequest request) {
        return read(request, props.accessName());
    }

    private Optional<String> read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                return Optional.of(c.getValue());
            }
        }
        return Optional.empty();
    }

    private ResponseCookie build(String name, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(props.secure())
                .path(cookiePath)
                .sameSite(props.sameSite())
                .maxAge(maxAge);
        if (props.domain() != null && !props.domain().isBlank()) {
            b.domain(props.domain());
        }
        return b.build();
    }

    public String header() {
        return HttpHeaders.SET_COOKIE;
    }
}
