package com.hotel.core.repository;

import com.hotel.core.config.DatabaseConnection;
import com.hotel.core.model.StoredStaffAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcUserRepository implements UserRepository {

    private static final String SELECT_BY_USERNAME =
            "SELECT id, username, password_hash, full_name, role, is_enabled "
                    + "FROM users WHERE username = ?";

    @Override
    public Optional<StoredStaffAccount> findByUsername(String username) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_USERNAME)) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new StoredStaffAccount(
                        resultSet.getInt("id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("full_name"),
                        resultSet.getString("role"),
                        resultSet.getBoolean("is_enabled")));
            }
        }
    }

    @Override
    public void create(StoredStaffAccount account) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, is_enabled) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getUsername());
            statement.setString(2, account.getPasswordHash());
            statement.setString(3, account.getFullName());
            statement.setString(4, account.getRole());
            statement.setBoolean(5, account.isEnabled());
            statement.executeUpdate();
        }
    }
}
