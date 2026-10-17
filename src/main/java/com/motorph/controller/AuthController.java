package com.motorph.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger audit = LoggerFactory.getLogger("AUDIT");
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest body,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        if (body == null || body.username() == null || body.password() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and password are required"));
        }
        try {
            UsernamePasswordAuthenticationToken token =
                    UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password());
            token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            Authentication auth = authenticationManager.authenticate(token);

            if (request.getSession(false) != null) {
                request.changeSessionId(); // session-fixation protection (R-06)
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contextRepository.saveContext(context, request, response);

            audit.info("action=LOGIN_SUCCESS actor={} ip={} timestamp={}",
                    auth.getName(), request.getRemoteAddr(), Instant.now());
            return ResponseEntity.ok(Map.of("username", auth.getName(),
                    "authorities", auth.getAuthorities().toString()));
        } catch (AuthenticationException e) {
            audit.info("action=LOGIN_FAILED actor={} ip={} timestamp={}",
                    body.username(), request.getRemoteAddr(), Instant.now());
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }
}