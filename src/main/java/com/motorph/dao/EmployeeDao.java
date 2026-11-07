package com.motorph.dao;

import com.motorph.model.Employee;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Same pattern as UserDao: every query is a PreparedStatement with bound
 * parameters (R-01) and try-with-resources closes every resource (R-04).
 */
@Repository
public class EmployeeDao {

    private static final String COLUMNS = "e.id, e.full_name, e.job_title, e.basic_salary, e.allowance";

    private final DataSource dataSource;

    public EmployeeDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Own-record lookup: the employee is found through the logged-in username, never through a URL id. */
    public Optional<Employee> findByUsername(String username) {
        String sql = "SELECT " + COLUMNS + " FROM employee e "
                + "JOIN app_user u ON u.employee_id = e.id WHERE u.username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while looking up employee", e);
        }
    }

    public Optional<Employee> findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM employee e WHERE e.id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while looking up employee", e);
        }
    }

    public List<Employee> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM employee e ORDER BY e.id";
        List<Employee> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing employees", e);
        }
    }

    public int updateBasicSalary(long id, BigDecimal newSalary) {
        String sql = "UPDATE employee SET basic_salary = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newSalary);
            stmt.setLong(2, id);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while updating salary", e);
        }
    }

    private static Employee map(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getLong("id"),
                rs.getString("full_name"),
                rs.getString("job_title"),
                rs.getBigDecimal("basic_salary"),
                rs.getBigDecimal("allowance"));
    }
}
