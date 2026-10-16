package com.hotel.core.setup;

import com.hotel.core.model.StoredStaffAccount;
import com.hotel.core.repository.JdbcUserRepository;
import com.hotel.core.repository.UserRepository;
import com.hotel.core.service.AuthenticationService;
import com.hotel.core.service.PasswordHasher;

import java.io.Console;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

/** One-time interactive setup for a staff receptionist account. */
public final class StaffAccountSetup {

    private static final String RECEPTIONIST_ROLE = "Receptionist";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public StaffAccountSetup(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    public void createReceptionist(String username, String fullName, char[] password) throws SQLException {
        AuthenticationService.validateNewAccount(username, fullName);
        String passwordHash = passwordHasher.hash(password);
        userRepository.create(new StoredStaffAccount(
                0, username.trim(), passwordHash, fullName.trim(), RECEPTIONIST_ROLE, true));
    }

    public static void main(String[] args) throws SQLException {
        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Run this setup utility in a terminal that supports password input");
        }

        String username = console.readLine("Receptionist username: ");
        String fullName = console.readLine("Receptionist display name: ");
        char[] password = null;
        char[] confirmation = null;
        try {
            password = console.readPassword("Password (12-128 characters): ");
            confirmation = console.readPassword("Confirm password: ");
            if (password == null || confirmation == null) {
                throw new IllegalStateException("Password input was interrupted");
            }
            if (!Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("The passwords do not match");
            }
            new StaffAccountSetup(new JdbcUserRepository(), new PasswordHasher())
                    .createReceptionist(username, fullName, password);
            console.printf("Receptionist account created.%n");
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
