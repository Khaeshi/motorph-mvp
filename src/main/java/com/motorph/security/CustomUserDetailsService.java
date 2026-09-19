package com.motorph.security;

import com.motorph.dao.UserDao;
import com.motorph.model.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * R-08 evidence: every lookup logs a structured line (actor, action,
 * timestamp) via a dedicated "AUDIT" logger, configured in
 * logback-spring.xml to write to logs/audit.log. Run a login attempt,
 * then open that file — that IS your evidence for the R-08 audit row.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    // Separate named logger so audit lines can be routed to their own
    // file/appender independent of normal application logs.
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    private final UserDao userDao;

    public CustomUserDetailsService(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AppUser> found = userDao.findByUsername(username);

        if (found.isEmpty()) {
            audit.info("action=LOGIN_ATTEMPT actor={} result=USER_NOT_FOUND timestamp={}",
                    username, Instant.now());
            throw new UsernameNotFoundException("No user found: " + username);
        }

        AppUser user = found.get();
        audit.info("action=LOGIN_ATTEMPT actor={} result=USER_FOUND role={} timestamp={}",
                user.getUsername(), user.getRole(), Instant.now());

        return User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash()) // already a BCrypt hash
                .roles(user.getRole())
                .build();
    }
}
