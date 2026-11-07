package com.motorph.security;

import java.io.IOException;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Control 4 for the browser (form) login path and for denials/logout.
 * The JSON login path already logs LOGIN_SUCCESS / LOGIN_FAILED in AuthController.
 */
public final class AuditSecurityHandlers {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    private AuditSecurityHandlers() {}

    private static String clean(String s) {
        return s == null ? "unknown" : s.replaceAll("[\\r\\n]", "_");
    }

    /** 403 denials (wrong role, or a failed CSRF check). */
    public static class AccessDenied implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           AccessDeniedException ex) throws IOException {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String actor = (auth != null) ? auth.getName() : "anonymous";
            audit.info("action=ACCESS_DENIED actor={} request={} {} ip={} timestamp={}",
                    actor, request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), Instant.now());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
        }
    }

    public static class Logout implements LogoutHandler {
        @Override
        public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
            String actor = (authentication != null) ? authentication.getName() : "anonymous";
            audit.info("action=LOGOUT actor={} ip={} timestamp={}", actor, request.getRemoteAddr(), Instant.now());
        }
    }

    public static class LoginSuccess extends SavedRequestAwareAuthenticationSuccessHandler {
        public LoginSuccess() {
            setDefaultTargetUrl("/home");
            setAlwaysUseDefaultTargetUrl(true);
        }

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                            Authentication authentication) throws IOException, ServletException {
            audit.info("action=LOGIN_SUCCESS actor={} ip={} timestamp={}",
                    authentication.getName(), request.getRemoteAddr(), Instant.now());
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    public static class LoginFailure extends SimpleUrlAuthenticationFailureHandler {
        public LoginFailure() {
            super("/login?error");
        }

        @Override
        public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                            AuthenticationException exception) throws IOException, ServletException {
            audit.info("action=LOGIN_FAILED actor={} ip={} timestamp={}",
                    clean(request.getParameter("username")), request.getRemoteAddr(), Instant.now());
            super.onAuthenticationFailure(request, response, exception);
        }
    }
}
