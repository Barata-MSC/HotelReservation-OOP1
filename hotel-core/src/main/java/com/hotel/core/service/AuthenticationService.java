package com.hotel.core.service;

import com.hotel.core.exception.InvalidCredentialsException;
import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.StaffUser;
import com.hotel.core.model.StoredStaffAccount;
import com.hotel.core.repository.JdbcUserRepository;
import com.hotel.core.repository.UserRepository;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public final class AuthenticationService {

    private static final int MAX_USERNAME_LENGTH = 100;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticationService() {
        this(new JdbcUserRepository(), new PasswordHasher());
    }

    public AuthenticationService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    public StaffUser authenticate(String username, char[] password) throws SQLException {
        if (username == null || username.isBlank() || username.length() > MAX_USERNAME_LENGTH
                || password == null || password.length == 0 || password.length > 128) {
            throw new InvalidCredentialsException();
        }

        Optional<StoredStaffAccount> account = userRepository.findByUsername(username.trim());
        if (account.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        StoredStaffAccount stored = account.get();
        boolean passwordMatches = passwordHasher.verify(password, stored.getPasswordHash());
        if (!stored.isEnabled() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return stored.toStaffUser();
    }

    public static void validateNewAccount(String username, String fullName) {
        if (username == null || username.isBlank() || username.trim().length() > MAX_USERNAME_LENGTH) {
            throw new ValidationException("Username must contain 1 to 100 characters");
        }
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 150) {
            throw new ValidationException("Full name must contain 1 to 150 characters");
        }
    }
}
