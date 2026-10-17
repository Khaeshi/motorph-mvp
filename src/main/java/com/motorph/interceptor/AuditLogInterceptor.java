package com.motorph.interceptor;

import java.time.Instant;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** R-08 Maker-Checker: write requests need a different approver (X-Checker-Id header). */
@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String ATTR_CHECKER = "audit.checker";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if (!WRITE_METHODS.contains(req.getMethod())) {
            return true;
        }
        String maker = currentUser();
        String checker = req.getHeader("X-Checker-Id");
        if (checker == null || checker.isBlank()) {
            return reject(req, res, maker, "none", "MISSING_CHECKER");
        }
        checker = checker.trim();
        if (checker.equalsIgnoreCase(maker)) {
            return reject(req, res, maker, checker, "SELF_APPROVAL");
        }
        req.setAttribute(ATTR_CHECKER, checker);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        Object checker = req.getAttribute(ATTR_CHECKER);
        if (checker == null) {
            return; // rejected requests were already logged in preHandle
        }
        String result = (ex == null && res.getStatus() < 400) ? "EXECUTED" : "FAILED";
        audit.info("action=MAKER_CHECKER_{} actor={} approver={} request={} {} status={} ip={} timestamp={}",
                result, currentUser(), checker, req.getMethod(), req.getRequestURI(),
                res.getStatus(), req.getRemoteAddr(), Instant.now());
    }

    private boolean reject(HttpServletRequest req, HttpServletResponse res,
                           String maker, String checker, String reason) throws Exception {
        audit.info("action=MAKER_CHECKER_REJECTED actor={} approver={} request={} {} reason={} ip={} timestamp={}",
                maker, checker, req.getMethod(), req.getRequestURI(), reason, req.getRemoteAddr(), Instant.now());
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"Maker-checker approval required\",\"reason\":\"" + reason + "\"}");
        return false;
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}