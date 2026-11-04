package com.api.batterymantra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces canonical domain and HTTPS redirect (HTTP 301 Permanent Redirect):
 * - Redirects all www traffic: https://www.batterymantra.com/* -> https://batterymantra.com/*
 * - Redirects all batterymantra.in traffic: http(s)://(www.)batterymantra.in/* -> https://batterymantra.com/*
 * - Redirects all HTTP traffic to HTTPS: http://batterymantra.com/* -> https://batterymantra.com/*
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CanonicalRedirectFilter extends OncePerRequestFilter {

    private static final String CANONICAL_HOST = "batterymantra.com";
    private static final String CANONICAL_BASE_URL = "https://batterymantra.com";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String hostHeader = request.getHeader("X-Forwarded-Host");
        if (hostHeader == null || hostHeader.isBlank()) {
            hostHeader = request.getHeader("Host");
        }
        if (hostHeader == null || hostHeader.isBlank()) {
            hostHeader = request.getServerName();
        }

        // Strip port if present in host header (e.g. www.batterymantra.com:443)
        String host = hostHeader != null ? hostHeader.split(":")[0].trim().toLowerCase() : "";

        String protoHeader = request.getHeader("X-Forwarded-Proto");
        if (protoHeader == null || protoHeader.isBlank()) {
            protoHeader = request.getScheme();
        }
        String scheme = protoHeader != null ? protoHeader.trim().toLowerCase() : "https";

        boolean isTargetHost = host.equals("www.batterymantra.com")
                || host.equals("batterymantra.in")
                || host.equals("www.batterymantra.in");

        boolean isCanonicalHostHttp = host.equals(CANONICAL_HOST) && "http".equalsIgnoreCase(scheme);

        if (isTargetHost || isCanonicalHostHttp) {
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            String targetUrl = CANONICAL_BASE_URL + (uri != null ? uri : "");
            if (query != null && !query.isBlank()) {
                targetUrl += "?" + query;
            }

            log.info("Applying 301 Canonical Redirect: {}://{}{} -> {}", scheme, host, uri, targetUrl);

            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", targetUrl);
            response.setHeader("Connection", "close");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

