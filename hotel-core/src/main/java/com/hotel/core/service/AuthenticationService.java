package com.hotel.core.service;

import com.hotel.core.exception.InvalidCredentialsException;
import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.StoredUserAccount;
import com.hotel.core.model.UserRole;
import com.hotel.core.repository.JdbcUserRepository;
import com.hotel.core.repository.UserRepository;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class AuthenticationService {

    private static final int MAX_USERNAME_LENGTH = 100;
    private static final int MAX_EMAIL_LENGTH = 254;
    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$");
    private static final Pattern CONTACT = Pattern.compile("^[+0-9() .-]{7,30}$");
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticationService() {
        this(new JdbcUserRepository(), new PasswordHasher());
    }

    public AuthenticationService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "passwordHasher");
    }

    public AuthenticatedUser authenticate(String identifier, char[] password) throws SQLException {
        if (identifier == null || identifier.isBlank() || identifier.length() > MAX_EMAIL_LENGTH
                || password == null || password.length == 0
                || password.length > PasswordHasher.MAX_PASSWORD_LENGTH) {
            throw new InvalidCredentialsException();
        }

        Optional<StoredUserAccount> account = userRepository.findByUsernameOrEmail(identifier.trim());
        if (account.isEmpty()) {
            throw new InvalidCredentialsException();
        }

        StoredUserAccount stored = account.get();
        boolean passwordMatches = passwordHasher.verify(password, stored.getPasswordHash());
        if (!stored.isEnabled() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return stored.toAuthenticatedUser();
    }

    public AuthenticatedUser registerCustomer(
            String fullName, String email, String contactNumber, char[] password) throws SQLException {
        validateCustomerAccount(fullName, email, contactNumber);
        if (password == null || password.length < PasswordHasher.MIN_PASSWORD_LENGTH
                || password.length > PasswordHasher.MAX_PASSWORD_LENGTH) {
            throw new ValidationException(
                    "Password must be between " + PasswordHasher.MIN_PASSWORD_LENGTH
                            + " and " + PasswordHasher.MAX_PASSWORD_LENGTH + " characters.");
        }
        if (userRepository.findByUsernameOrEmail(email.trim()).isPresent()) {
            throw new ValidationException("An account already exists for that email.");
        }

        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        String passwordHash = passwordHasher.hash(password);
        StoredUserAccount customer = new StoredUserAccount(
                0,
                normalizedEmail,
                passwordHash,
                fullName.trim(),
                normalizedEmail,
                contactNumber.trim(),
                UserRole.CUSTOMER,
                true);
        try {
            customer = userRepository.create(customer);
        } catch (SQLIntegrityConstraintViolationException exception) {
            throw new ValidationException("An account already exists for that email.");
        }
        return customer.toAuthenticatedUser();
    }

    public static void validateNewAccount(String username, String fullName) {
        if (username == null || username.isBlank() || username.trim().length() > MAX_USERNAME_LENGTH) {
            throw new ValidationException("Username must contain 1 to 100 characters");
        }
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 150) {
            throw new ValidationException("Full name must contain 1 to 150 characters");
        }
    }

    public static void validateCustomerAccount(
            String fullName, String email, String contactNumber) {
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 150) {
            throw new ValidationException("Full name must contain 1 to 150 characters.");
        }
        if (email == null || email.isBlank() || email.trim().length() > MAX_EMAIL_LENGTH
                || !EMAIL.matcher(email.trim()).matches()) {
            throw new ValidationException("Enter a valid email address.");
        }
        if (contactNumber == null) {
            throw new ValidationException("Enter a valid contact number.");
        }
        String normalizedContact = contactNumber.trim();
        long digitCount = normalizedContact.chars()
                .filter(character -> character >= '0' && character <= '9')
                .count();
        if (!CONTACT.matcher(normalizedContact).matches() || digitCount < 7 || digitCount > 15) {
            throw new ValidationException("Enter a valid contact number.");
        }
    }
}
