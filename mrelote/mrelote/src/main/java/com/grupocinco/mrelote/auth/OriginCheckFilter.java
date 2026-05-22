package com.grupocinco.mrelote.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

/**
 * Same-origin guard for state-changing requests. Defense-in-depth on top of
 * SameSite=Strict cookies — rejects requests whose Origin/Referer header does
 * not match the configured allowed origins.
 */
public class OriginCheckFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name()
    );

    private final List<String> allowedOrigins;

    public OriginCheckFilter(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (SAFE_METHODS.contains(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String source = originOf(request);
        if (source == null || !allowedOrigins.contains(source)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cross-site request blocked");
            return;
        }

        chain.doFilter(request, response);
    }

    private String originOf(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.isBlank()) {
            return origin;
        }
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(referer);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            if (scheme == null || host == null) {
                return null;
            }
            return port == -1
                    ? scheme + "://" + host
                    : scheme + "://" + host + ":" + port;
        } catch (URISyntaxException ex) {
            return null;
        }
    }
}
