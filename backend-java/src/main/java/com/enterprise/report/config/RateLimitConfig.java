package com.enterprise.report.config;

import com.enterprise.report.security.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * API Rate Limiting filter using Redis sliding window.
 * Default: 100 requests per minute per tenant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitConfig extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_LIMIT = 100;
    private static final int WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Skip rate limiting for non-API endpoints
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") || path.startsWith("/api/auth/refresh")) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId == null || tenantId.isEmpty()) {
            tenantId = "anonymous";
        }

        String key = "rate_limit:" + tenantId + ":" + (System.currentTimeMillis() / (WINDOW_SECONDS * 1000));

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }

            if (count != null && count > DEFAULT_LIMIT) {
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                Map<String, Object> body = new HashMap<>();
                body.put("code", 429);
                body.put("message", "Too many requests. Please try again later.");
                body.put("retryAfter", WINDOW_SECONDS);
                response.getWriter().write(objectMapper.writeValueAsString(body));
                return;
            }

            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(DEFAULT_LIMIT));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, DEFAULT_LIMIT - (count != null ? count : 0))));
        } catch (Exception e) {
            log.warn("Rate limit check failed: {}", e.getMessage());
            // If Redis is down, allow the request through
        }

        filterChain.doFilter(request, response);
    }
}
