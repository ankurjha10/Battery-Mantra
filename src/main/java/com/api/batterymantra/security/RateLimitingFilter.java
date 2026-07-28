package com.api.batterymantra.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ProxyManager<byte[]> proxyManager;

    public RateLimitingFilter(ProxyManager<byte[]> proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (isRateLimitedEndpoint(path) && request.getMethod().equalsIgnoreCase("POST")) {
            String clientIp = getClientIp(request);
            
            Supplier<BucketConfiguration> bucketConfigurationSupplier = () -> BucketConfiguration.builder()
                    .addLimit(Bandwidth.builder().capacity(15).refillGreedy(15, Duration.ofMinutes(1)).build())
                    .build();

            Bucket bucket = proxyManager.builder().build(clientIp.getBytes(StandardCharsets.UTF_8), bucketConfigurationSupplier);

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
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
}
