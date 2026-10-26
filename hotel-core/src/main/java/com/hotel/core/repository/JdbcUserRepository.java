package com.hotel.core.repository;

import com.hotel.core.config.DatabaseConnection;
import com.hotel.core.model.StoredUserAccount;
import com.hotel.core.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class JdbcUserRepository implements UserRepository {

    private static final String SELECT_BY_IDENTIFIER =
            "SELECT id, username, password_hash, full_name, email, contact_number, role, is_enabled "
                    + "FROM users WHERE username = ? OR email = ?";

    @Override
    public Optional<StoredUserAccount> findByUsernameOrEmail(String identifier) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_IDENTIFIER)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                StoredUserAccount account = new StoredUserAccount(
                        resultSet.getInt("id"),
                        resultSet.getString("username"),
                        resultSet.getString("password_hash"),
                        resultSet.getString("full_name"),
                        resultSet.getString("email"),
                        resultSet.getString("contact_number"),
                        UserRole.fromDatabaseValue(resultSet.getString("role")),
                        resultSet.getBoolean("is_enabled"));
                if (resultSet.next()) {
                    throw new SQLException("The login identifier matches more than one account.");
                }
                return Optional.of(account);
            }
        }
    }

    @Override
    public StoredUserAccount create(StoredUserAccount account) throws SQLException {
        String sql = "INSERT INTO users "
                + "(username, password_hash, full_name, email, contact_number, role, is_enabled) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, account.getUsername());
            statement.setString(2, account.getPasswordHash());
            statement.setString(3, account.getFullName());
            statement.setString(4, account.getEmail());
            statement.setString(5, account.getContactNumber());
            statement.setString(6, account.getRole().getDatabaseValue());
            statement.setBoolean(7, account.isEnabled());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The account was not created.");
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("The account was created without returning its identifier.");
                }
                return account.withId(generatedKeys.getInt(1));
            }
        }
    }
}
