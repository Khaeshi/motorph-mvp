package com.motorph.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Control 2: 5 FAILED login attempts per source IP per 15 minutes.
 * A token is taken on every attempt (so a concurrent burst is still cut off at 5)
 * and handed back when that attempt turns out to be a successful login.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    // one bucket per source IP (in-memory)
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(MAX_ATTEMPTS, Refill.intervally(MAX_ATTEMPTS, WINDOW));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean loginPost = "POST".equals(request.getMethod())
                && ("/api/v1/auth/login".equals(path) || "/login".equals(path));
        return !loginPost;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            audit.info("action=LOGIN_BLOCKED ip={} path={} result=HTTP_429 retryAfterSeconds={} timestamp={}",
                    ip, request.getRequestURI(), retryAfter, Instant.now());

            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many login attempts. Try again later.\"}");
            return;
        }

        Authentication before = SecurityContextHolder.getContext().getAuthentication();
        chain.doFilter(request, response);
        Authentication after = SecurityContextHolder.getContext().getAuthentication();

        // A successful login leaves a NEW authenticated object in the context.
        // Comparing with the one from before the request means a failed guess made
        // while already logged in is not mistaken for a success.
        if (loginSucceeded(before, after)) {
            bucket.addTokens(1);
        }
    }

    private static boolean loginSucceeded(Authentication before, Authentication after) {
        return after != null
                && after != before
                && after.isAuthenticated()
                && !(after instanceof AnonymousAuthenticationToken);
    }
}
