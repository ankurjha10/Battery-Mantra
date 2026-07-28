package com.api.batterymantra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 15;
    private static final long ONE_MINUTE_IN_MILLIS = 60000;

    private final Map<String, RateLimitInfo> requestCounts = new ConcurrentHashMap<>();

    private static class RateLimitInfo {
        int count;
        long windowStart;

        public RateLimitInfo(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (isRateLimitedEndpoint(path) && request.getMethod().equalsIgnoreCase("POST")) {
            String clientIp = getClientIp(request);
            if (isRateLimited(clientIp)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":429,\"message\":\"Too many requests. Please try again after a minute.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/callbacks") ||
               path.startsWith("/api/enquiries");
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private boolean isRateLimited(String clientIp) {
        long currentTime = System.currentTimeMillis();
        boolean[] isLimited = new boolean[1];

        requestCounts.compute(clientIp, (key, info) -> {
            if (info == null || (currentTime - info.windowStart) > ONE_MINUTE_IN_MILLIS) {
                return new RateLimitInfo(1, currentTime);
            }
            if (info.count >= MAX_REQUESTS_PER_MINUTE) {
                isLimited[0] = true;
                return info;
            }
            info.count++;
            return info;
        });

        return isLimited[0];
    }
}
