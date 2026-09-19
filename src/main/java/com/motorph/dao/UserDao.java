package com.motorph.dao;

import com.motorph.model.AppUser;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * R-01 evidence: every query here uses a PreparedStatement with bound
 * parameters ("?") — never string concatenation — so user-supplied input
 * can never be interpreted as SQL. This is the exact control described in
 * IAS 1 (R-01, dual-layer input hardening).
 *
 * try-with-resources on Connection/PreparedStatement/ResultSet also
 * demonstrates R-04 (deterministic resource lifecycle mgmt).
 */
@Repository
public class UserDao {

    private final DataSource dataSource;

    public UserDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<AppUser> findByUsername(String username) {
        String sql = "SELECT username, password_hash, role FROM app_user WHERE username = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username); // bound parameter — not concatenated into the SQL string

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    AppUser user = new AppUser(
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getString("role")
                    );
                    return Optional.of(user);
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database error while looking up user", e);
        }
    }
}
