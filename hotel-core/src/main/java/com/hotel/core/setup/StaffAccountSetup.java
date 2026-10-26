package com.hotel.core.setup;

import com.hotel.core.model.StoredUserAccount;
import com.hotel.core.model.UserRole;
import com.hotel.core.repository.JdbcUserRepository;
import com.hotel.core.repository.UserRepository;
import com.hotel.core.service.AuthenticationService;
import com.hotel.core.service.PasswordHasher;

import java.io.Console;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

/** One-time interactive setup for an administrator account. */
public final class StaffAccountSetup {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public StaffAccountSetup(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    public void createAdmin(String username, String fullName, char[] password) throws SQLException {
        AuthenticationService.validateNewAccount(username, fullName);
        String passwordHash = passwordHasher.hash(password);
        userRepository.create(new StoredUserAccount(
                0, username.trim(), passwordHash, fullName.trim(),
                null, null, UserRole.ADMIN, true));
    }

    public static void main(String[] args) throws SQLException {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Run this setup utility in a terminal that supports password input");
        }

        String username = console.readLine("Admin username: ");
        String fullName = console.readLine("Admin display name: ");
        char[] password = null;
        char[] confirmation = null;
        try {
            password = console.readPassword(
                    "Password (" + PasswordHasher.MIN_PASSWORD_LENGTH + "-128 characters): ");
            confirmation = console.readPassword("Confirm password: ");
            if (password == null || confirmation == null) {
                throw new IllegalStateException("Password input was interrupted");
            }
            if (!Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("The passwords do not match");
            }
            new StaffAccountSetup(new JdbcUserRepository(), new PasswordHasher())
                    .createAdmin(username, fullName, password);
            console.printf("Admin account created.%n");
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
    }
}
